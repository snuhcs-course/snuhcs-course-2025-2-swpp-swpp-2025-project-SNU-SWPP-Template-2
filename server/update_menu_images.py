#!/usr/bin/env python3
"""
메뉴 이미지 URL 메타데이터 업데이트 스크립트

이 스크립트는 기존 ChromaDB 메뉴 컬렉션에 이미지 URL 메타데이터를 추가합니다.
전체 인덱싱을 재수행하지 않고 기존 임베딩을 유지하면서 메타데이터만 업데이트합니다.
"""

import os
import sys
import logging
from pathlib import Path

# Django 설정
os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'config.settings')
import django
django.setup()

from recommendation_system.chroma_index import ChromaVectorIndexBuilder
from recommendation_system import EmbeddingService

def main():
    """메인 실행 함수"""
    # 로깅 설정
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    logger = logging.getLogger(__name__)
    
    logger.info("🚀 메뉴 이미지 URL 메타데이터 업데이트 시작")
    
    try:
        # 임베딩 서비스 초기화
        logger.info("📦 임베딩 서비스 초기화 중...")
        embedding_service = EmbeddingService()
        
        # ChromaDB 벡터 인덱스 빌더 초기화
        logger.info("🗄️ ChromaDB 벡터 인덱스 빌더 초기화 중...")
        vector_builder = ChromaVectorIndexBuilder(embedding_service, './chroma_db')
        
        # 컬렉션 정보 확인
        info = vector_builder.get_collection_info()
        logger.info(f"📊 현재 컬렉션 정보: {info}")
        
        if info.get("menu_collection", {}).get("count", 0) == 0:
            logger.error("❌ 메뉴 컬렉션이 비어있습니다. 먼저 인덱스를 구축해주세요.")
            return False
        
        # 원본 JSON 파일 경로
        json_file_path = "/Users/jaejoon/swpp-2025-project-team-13/server/restaurant/management/commands/관악구_음식점_상세_전체.json"
        
        if not os.path.exists(json_file_path):
            logger.error(f"❌ 원본 JSON 파일을 찾을 수 없습니다: {json_file_path}")
            return False
        
        # 이미지 URL 매핑 생성
        logger.info("🖼️ 이미지 URL 매핑 생성 중...")
        image_urls_mapping = vector_builder.create_image_urls_mapping(json_file_path)
        
        if not image_urls_mapping:
            logger.error("❌ 이미지 URL 매핑 생성 실패")
            return False
        
        logger.info(f"✅ 이미지 URL 매핑 생성 완료: {len(image_urls_mapping)}개 메뉴")
        
        # 메타데이터 배치 업데이트
        logger.info("🔄 메타데이터 배치 업데이트 시작...")
        success = vector_builder.batch_update_menu_metadata(image_urls_mapping, batch_size=50)
        
        if success:
            logger.info("🎉 메뉴 이미지 URL 메타데이터 업데이트 완료!")
            
            # 업데이트 후 컬렉션 정보 확인
            updated_info = vector_builder.get_collection_info()
            logger.info(f"📊 업데이트 후 컬렉션 정보: {updated_info}")
            
            return True
        else:
            logger.error("❌ 메타데이터 업데이트 실패")
            return False
            
    except Exception as e:
        logger.error(f"❌ 업데이트 중 오류 발생: {e}")
        return False

if __name__ == "__main__":
    success = main()
    sys.exit(0 if success else 1)
