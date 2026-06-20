from .extract_style_profile import compute_style_profile_text
from sentence_transformers import SentenceTransformer
import numpy as np
import json

model = SentenceTransformer("jhgan/ko-sroberta-multitask")

def l2norm(x):
    return x / (np.linalg.norm(x, axis=-1, keepdims=True) + 1e-12)

def embed_sentences(sentences):
    E = model.encode(sentences, convert_to_numpy=True, batch_size=64)
    return l2norm(E)

def diary_embedding(diary_text):
    sentences = [s.strip() for s in diary_text.split("\n") if s.strip()]
    E = embed_sentences(sentences)
    diary_vec = l2norm(E.mean(axis=0, keepdims=True))[0]
    
    return diary_vec

def compute_style_vector(diaries):
    diary_vecs = [diary_embedding(d) for d in diaries]
    user_vec = np.stack(diary_vecs, axis=0).mean(axis=0)
    user_vec = l2norm(user_vec.reshape(1, -1))[0]
    return user_vec.tolist()

def extract_style_examples(diaries, style_vector, n=4):
    text = "\n".join(diaries)

    raw_candidates = []
    for paragraph in text.split("\n"):
        paragraph = paragraph.strip()
        if len(paragraph) < 2:
            continue
        sentences = [s.strip() for s in paragraph.split(".") if len(s.strip()) >= 8]
        raw_candidates.extend(sentences)

    if len(raw_candidates) <= n:
        return raw_candidates

    candidate_vectors = embed_sentences(raw_candidates)
    style_vector = np.array(style_vector).reshape(1,-1)

    similarities = (candidate_vectors @ style_vector.T).reshape(-1)
    top_idx = np.argsort(-similarities)[:n]

    return [raw_candidates[i] for i in top_idx]

def extract_style(diaries):
    style_vector = compute_style_vector(diaries)
    style_examples = extract_style_examples(diaries, style_vector, n=4)
    style_prompt = compute_style_profile_text(diaries)

    return {
        "style_vector": style_vector,
        "style_examples": style_examples,
        "style_prompt": style_prompt
    }
