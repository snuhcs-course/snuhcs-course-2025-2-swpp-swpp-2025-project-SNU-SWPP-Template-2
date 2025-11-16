"""Static fallback data for book publication recommendations."""

from __future__ import annotations

SAMPLE_PUBLICATIONS: list[dict[str, object]] = [
    {
        "identifier": "sample-norwegian-wood",
        "title": "노르웨이의 숲",
        "authors": ["무라카미 하루키"],
        "genres": ["문학", "청춘", "로맨스"],
        "description": (
            "1960년대 도쿄를 배경으로 상실과 성장, 사랑의 양가성을 그려낸 "
            "청춘 소설입니다. 와타나베, 나오코, 미도리의 관계 속에서 삶과 죽음의"
            " 균형을 찾아가는 여정을 섬세한 문장으로 담아냅니다."
        ),
        "cover_image": None,
    },
    {
        "identifier": "sample-project-hail-mary",
        "title": "Project Hail Mary",
        "authors": ["Andy Weir"],
        "genres": ["Science Fiction", "Space Opera"],
        "description": (
            "지구 멸망을 막기 위해 기억을 잃은 과학자가 외딴 우주선에서 깨어나 "
            "새로운 외계 생명체와 협력하는 과정을 그린 하드 SF 소설입니다."
        ),
        "cover_image": None,
    },
    {
        "identifier": "sample-midnight-library",
        "title": "The Midnight Library",
        "authors": ["Matt Haig"],
        "genres": ["Fantasy", "Self-Help"],
        "description": (
            "살아야 할 이유를 잃은 주인공 노라가 삶과 죽음 사이의 도서관에서 "
            "수많은 평행 우주를 경험하며 자기 회복과 선택의 의미를 발견하는 이야기입니다."
        ),
        "cover_image": None,
    },
    {
        "identifier": "sample-pachinko",
        "title": "파친코",
        "authors": ["이민진"],
        "genres": ["Historical Fiction", "Family Saga"],
        "description": (
            "일제강점기부터 1980년대까지 재일조선인 가족 4대의 삶을 다루며 정체성과 "
            "차별, 생존을 탐구하는 대하 드라마입니다."
        ),
        "cover_image": None,
    },
]
