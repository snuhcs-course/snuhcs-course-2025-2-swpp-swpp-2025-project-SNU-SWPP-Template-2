"""
데이터 처리 및 인덱스 구축 관리 명령어

이 명령어는 JSON 데이터를 처리하고 ChromaDB 인덱스를 구축합니다.
"""

import json
import logging
from django.core.management.base import BaseCommand, CommandError
from pathlib import Path
import os

from recommendation_system import (
    load_restaurant_data, 
    process_restaurant_data, 
    EmbeddingService, 
    VectorIndexBuilder
)

logger = logging.getLogger(__name__)

class Command(BaseCommand):
    help = '음식점 데이터를 처리하고 추천 인덱스를 구축합니다'
    
    def add_arguments(self, parser):
        parser.add_argument(
            '--json-file',
            type=str,
            help='처리할 JSON 파일 경로',
            default='restaurant/management/commands/장블랑제리_상세.json'
        )
        parser.add_argument(
            '--output-dir',
            type=str,
            help='인덱스 파일 출력 디렉토리',
            default='.'
        )
        parser.add_argument(
            '--model-name',
            type=str,
            help='사용할 임베딩 모델명',
            default='jhgan/ko-sbert-sts'
        )
        parser.add_argument(
            '--batch-size',
            type=int,
            help='배치 처리 크기',
            default=100
        )
    
    def handle(self, *args, **options):
        try:
            # 옵션 파싱
            json_file = options['json_file']
            output_dir = options['output_dir']
            model_name = options['model_name']
            batch_size = options['batch_size']
            
            self.stdout.write(
                self.style.SUCCESS(f'🚀 음식점 데이터 처리 시작: {json_file}')
            )
            
            # JSON 파일 경로 확인
            if not os.path.exists(json_file):
                raise CommandError(f'❌ JSON 파일을 찾을 수 없습니다: {json_file}')
            
            # 데이터 로드
            self.stdout.write('📂 데이터 로드 중...')
            restaurant_data = load_restaurant_data(json_file)
            self.stdout.write(
                self.style.SUCCESS(f'✅ 로드된 음식점 수: {len(restaurant_data):,}개')
            )
            
            # 데이터 처리
            self.stdout.write('⚙️  데이터 처리 중...')
            self.stdout.write('   - 메뉴 및 가게 문서 생성 중...')
            menu_documents, place_documents = process_restaurant_data(restaurant_data)
            self.stdout.write(
                self.style.SUCCESS(
                    f'✅ 처리 완료 - 메뉴: {len(menu_documents):,}개, 가게: {len(place_documents):,}개'
                )
            )
            
            # 임베딩 서비스 초기화
            self.stdout.write(f'🤖 임베딩 모델 로드 중: {model_name}')
            embedding_service = EmbeddingService(model_name)
            self.stdout.write('✅ 임베딩 모델 로드 완료')
            
            # 벡터 인덱스 빌더 초기화
            self.stdout.write('🔧 벡터 인덱스 구축 중...')
            vector_index_builder = VectorIndexBuilder(embedding_service)
            
            # 인덱스 구축
            self.stdout.write('📊 ChromaDB 인덱스 구축 시작...')
            self.stdout.write(f'   - 메뉴 인덱스: {len(menu_documents):,}개 문서 처리 중...')
            vector_index_builder.build_indices(menu_documents, place_documents)
            
            # 출력 디렉토리 생성
            os.makedirs(output_dir, exist_ok=True)
            
            # ChromaDB는 영구 저장소를 사용하므로 별도 저장 불필요
            self.stdout.write(self.style.SUCCESS('🎉 ChromaDB 인덱스 구축 완료!'))
            self.stdout.write('💾 인덱스는 영구 저장소에 자동 저장되었습니다.')
            self.stdout.write(f'📁 저장 위치: {os.path.abspath(output_dir)}')
            
        except Exception as e:
            logger.error(f'인덱스 구축 실패: {e}')
            raise CommandError(f'인덱스 구축 실패: {e}')
