#!/usr/bin/env python3
"""Rewrites the upstream NSFW ONNX model so onnx2tf can turn it into assets/models/nsfw.tflite.

Upstream: https://huggingface.co/taufiqdp/mobilenetv4_conv_small.e2400_r224_in1k_nsfw_classifier
(Apache-2.0), file mobilenetv4_conv_small.e2400_r224_in1k_nsfw_classifier.onnx. Its graph takes one
unbatched CHW image in 0..1 (despite declaring [batch, 224, 224]), normalises it in-graph and returns
logits. This script:
  1. declares the input as [1, 3, 224, 224] and turns the in-graph Unsqueeze into an Identity,
  2. reshapes the mean/std constants to [1, 3, 1, 1] so the NCHW->NHWC conversion transposes them,
  3. appends a Softmax so the output is probabilities.

Usage (in a venv with onnx==1.17.0, onnx2tf==1.26.3, tensorflow==2.18.1, tf-keras==2.18.0, run outside
the repo, with `python -I`):
  python -I prepare_nsfw_onnx.py upstream.onnx nsfw_fixed.onnx
  onnx2tf -i nsfw_fixed.onnx -o tfout      # -> tfout/nsfw_fixed_float32.tflite == nsfw.tflite
onnx2tf wants a calibration_image_sample_data_20x128x128x3_float32.npy in the working directory (it
otherwise downloads one); any 20x128x128x3 float32 array of natural images in 0..1 works, it is only
used for its own accuracy checks and does not change the weights.
"""
import sys

import onnx
from onnx import TensorProto, helper, numpy_helper


def main(src: str, dst: str) -> None:
    m = onnx.load(src)
    g = m.graph
    dims = g.input[0].type.tensor_type.shape.dim
    del dims[:]
    for v in (1, 3, 224, 224):
        dims.add().dim_value = v
    for n in g.node:
        if n.op_type == "Unsqueeze":
            del n.attribute[:]
            n.op_type = "Identity"
        elif n.op_type == "Constant" and n.output[0].startswith("/transforms/"):
            arr = numpy_helper.to_array(n.attribute[0].t)
            if arr.shape == (3, 1, 1):
                n.attribute[0].t.CopyFrom(numpy_helper.from_array(arr.reshape(1, 3, 1, 1)))
    g.node.append(helper.make_node("Softmax", [g.output[0].name], ["probs"], axis=1))
    del g.output[:]
    g.output.append(helper.make_tensor_value_info("probs", TensorProto.FLOAT, [1, 5]))
    onnx.checker.check_model(m)
    onnx.save(m, dst)


if __name__ == "__main__":
    main(sys.argv[1], sys.argv[2])
