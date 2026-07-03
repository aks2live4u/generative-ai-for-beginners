import re

STOPWORDS = {
    "the", "and", "a", "to", "of", "in", "for", "with", "on", "is", "are", "as",
    "at", "by", "an", "be", "this", "that", "will", "you", "your", "our", "we",
    "or", "from", "have", "has", "it", "their", "who", "role", "job", "team",
    "work", "experience", "skills", "years", "including", "etc", "such", "into",
    "across", "drive", "delivery", "enterprise", "background", "strong",
    "requires", "required", "requirements", "responsible", "responsibilities",
    "ability", "able", "working", "environment", "opportunity", "candidate",
    "candidates", "looking", "seeking", "join", "help", "make", "new", "all",
    "not", "can", "may", "if", "so", "but", "about", "also", "other", "more",
    "than", "when", "where", "what", "how", "well", "good", "great", "best",
    "us", "we're", "you'll", "we'll", "these", "those", "each", "every",
    "must", "should", "would", "could", "while", "within", "between",
    "using", "used", "use", "plus",
}


def extract_keywords(text: str, top_n: int = 30) -> list[str]:
    words = re.findall(r"[A-Za-z][A-Za-z0-9+.#/-]{1,}", text or "")
    freq: dict[str, int] = {}
    for word in words:
        lower = word.lower().strip(".,;:!?/-")
        if lower in STOPWORDS or len(lower) < 2:
            continue
        freq[lower] = freq.get(lower, 0) + 1
    ranked = sorted(freq.items(), key=lambda kv: kv[1], reverse=True)
    return [word for word, _ in ranked[:top_n]]


def keyword_match(resume_text: str, jd_text: str) -> tuple[list[str], list[str], float]:
    jd_keywords = set(extract_keywords(jd_text, top_n=40))
    resume_lower = (resume_text or "").lower()

    matched = [kw for kw in jd_keywords if kw in resume_lower]
    missing = [kw for kw in jd_keywords if kw not in resume_lower]
    pct = (len(matched) / len(jd_keywords) * 100) if jd_keywords else 0.0
    return sorted(matched), sorted(missing), round(pct, 1)


def flesch_reading_ease(text: str) -> float:
    """Approximate Flesch Reading Ease score (0-100, higher = easier to read)."""
    sentences = max(1, len(re.findall(r"[.!?]+", text or "")))
    words = re.findall(r"[A-Za-z']+", text or "")
    word_count = max(1, len(words))
    syllables = sum(_count_syllables(w) for w in words) or word_count

    score = 206.835 - 1.015 * (word_count / sentences) - 84.6 * (syllables / word_count)
    return round(max(0.0, min(100.0, score)), 1)


def _count_syllables(word: str) -> int:
    word = word.lower()
    vowels = "aeiouy"
    count = 0
    prev_was_vowel = False
    for char in word:
        is_vowel = char in vowels
        if is_vowel and not prev_was_vowel:
            count += 1
        prev_was_vowel = is_vowel
    if word.endswith("e") and count > 1:
        count -= 1
    return max(1, count)


def ats_score(resume_text: str, jd_text: str) -> float:
    _, _, keyword_pct = keyword_match(resume_text, jd_text)
    readability = flesch_reading_ease(resume_text)
    length_penalty = 0 if 200 <= len(resume_text.split()) <= 1200 else 10
    score = 0.7 * keyword_pct + 0.3 * min(readability, 100) - length_penalty
    return round(max(0.0, min(100.0, score)), 1)
