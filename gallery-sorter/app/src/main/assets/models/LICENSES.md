# Bundled models

Both models run fully on-device through LiteRT; nothing is downloaded at runtime.

## nsfw.tflite

- SHA-256: `38d04964c6a7d1ae57ca773b6c84c02d1930095c7fb552a7b00df30f8383e088` (9,971,180 bytes)
- Source: https://huggingface.co/taufiqdp/mobilenetv4_conv_small.e2400_r224_in1k_nsfw_classifier
  (revision `504317ad62086f357c9c5f70cf983726ff47efdb`), file
  `mobilenetv4_conv_small.e2400_r224_in1k_nsfw_classifier.onnx`,
  SHA-256 `46995c000cb285d0c0a0e5b58cc618012ed2e649d326d4fddae910951848fc40`.
- Licence: Apache-2.0 (declared in the model card metadata, `license: apache-2.0`).
- Model: timm `mobilenetv4_conv_small` fine-tuned as a 5-class NSFW classifier with the same classes as
  GantMan/nsfw_model: drawings, hentai, neutral, porn, sexy. Used in place of the GantMan MobileNetV2
  model, whose release files are on GitHub and not reachable from the build machine.
- Conversion: `tools/prepare_nsfw_onnx.py` (batched NCHW input, softmax appended), then
  `onnx2tf 1.26.3` (TensorFlow 2.18.1), float32 output. Reproducible: a second run gave the same
  SHA-256. Checked against the original ONNX with onnxruntime: max absolute difference in class
  probabilities 5e-6 over random and real images.
- Tensors: input `1×224×224×3` float32 RGB in 0..1 (ImageNet mean/std normalisation is inside the
  model); output `1×5` probabilities in the order above.
- Mapping used by the app: explicit = porn + hentai; suggestive = sexy; neutral and drawings are safe.

## face_embed.tflite

- SHA-256: `2046ab776fba2ec7e420767ffe7eb74a7402a8412d3d7ea8b9f4ca8420e5f3d7` (3,988,392 bytes), used as
  shipped (renamed from `mobile_facenet.tflite`).
- Source: Qualcomm AI Hub Models, MobileFaceNet v0.66.0, float TFLite:
  https://huggingface.co/qualcomm/MobileFaceNet →
  https://qaihub-public-assets.s3.us-west-2.amazonaws.com/qai-hub-models/models/mobile_facenet/releases/v0.66.0/mobile_facenet-tflite-float.zip
  (zip SHA-256 `92dc09420007cd24b45d64cc0e0bf9a7aabe3b7833579ec2e417fa0ec05253d3`).
- Licence: Apache-2.0 (model card metadata `license: apache-2.0`; its LICENSE file points to the original
  trained model's licence, https://github.com/foamliu/MobileFaceNet/blob/master/LICENSE, which is
  Apache License 2.0).
- Training data (upstream README): refined MS-Celeb-1M.
- Tensors: two inputs `img1`, `img2`, each `1×3×112×112` float32 RGB in 0..1, channels first; output
  `embeddings` `2×128` float32, one row per input, computed independently, not normalised (the app
  L2-normalises them).
