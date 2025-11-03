# Abstract
- This directory manages restaurant and menu information as postgresql database

# Directory structure
psql/
├── specifications.md       # this file
├── restaurants_gwanak.json # raw data
├── into_db.py              # redesign raw data into postgresql database
├── preprocess.py           # add allergen and taste info of each menu using langchain
├── client.py               # Restaurant recommendation system main module
├── test.py                 # Testing module
├── requirements.txt
├── db/
│   ├── schema.sql          # Postgresql to create tables
├── .env.example            # Template for database connection variables
├── docker-compose.yml      # for running PostgreSQL easily
└── README.md

# Explanation
## restarurants_gwanak.json
- a list of restaurant information
- a single entry consists of(but not limited to):
  - "id" : given id of restaurant
  - "name" : name of restaurant
  - "detail_info"
    - "x" : longitude
    - "y" : latitude
    - "menus" (as list)
      - "id" : menu id in the format {resetaurant_id}_{menu_index} 
      - "name" : menu name
      - "index" : menu index
      - "price" : menu price
      - "images": menu image url
      - "description" : menu description
    - "phone" : restaurant phone number
    - "group1", "group2", "group3" : restaurant location
    - "address"
    - "category"
    - "created_at"
    - "updated_at"
    - "place_images"
    - "road_address"
    - "category_code"
    - "category_code_list"
    - "visitor_review_stats"
      - "id"
      - "review"
        - "avgRating"
        - "totalCount"

## into_db.py
### Given
- DB schema in db/schema.sql
### Goal
- store data into most accessible and efficient form, considering the role of other files
- db_restaurant and db_menu

## preprocess.py
### Given
- db_restaurant
- db_menu
### Goal
- db_restaurant
  - extract values of the column category
  - categorize the list of categories

    | represented by | members |
    |:--------------:|---------|
    | 한식 | 곰탕/설렁탕, 국밥, 국수, 기사식당, 냉면, 덮밥, 두부요리, 막국수, 만두, 백반/가정식, 보리밥, 비빔밥, 순대/순댓국, 쌈밥, 아부찌부대찌개, 오므라이스, 죽, 찌개/전골, 추어탕, 칼국수/만두, 한식, 한식뷔페, 한정식, 해장국 |
    | 일식 | 돈가스, 일본식라면, 일식당, 일식튀김/꼬치, 초밥/롤 |
    | 분식 | 33떡볶이, 개성진찹쌀순대, 김밥, 떡볶이, 라면, 분식, 오니기리, 오뎅/꼬치, 전/빈대떡, 종합분식, 토스트, 핫도그 |
    | 중식 | 딤섬/중식만두, 마라탕, 중식당 |
    | 양식 | 스테이크/립, 스파게티/파스타전문, 스파게티스토리, 양식 |
    | 세계음식 | 멕시코/남미음식, 베트남음식, 스페인음식, 아시아음식, 이탈리아음식, 인도음식, 카레, 태국음식, 터키음식 |
    | 패스트푸드 | 서오릉피자, 피자, 햄버거, 후렌치후라이 |
    | 육류/고기요리 | 갈비탕, 감자탕, 고기뷔페, 곱창/막창/양, 닭갈비, 닭발, 닭볶음탕, 닭요리, 돼지고기구이, 백숙/삼계탕, 불닭, 사철/영양탕, 샤브샤브, 소고기구이, 양꼬치, 오리요리, 육류/고기요리, 장수통닭, 정육식당, 정육점, 족발/보쌈, 찜닭, 치킨/닭강정 |
    | 해산물 | 게요리, 굴요리, 낙지요리, 대게요리, 매운탕/해물탕, 복어요리, 생선구이, 생선회, 아귀찜, 해물찜, 오징어요리, 장어/먹장어요리, 조개요리, 주꾸미요리, 해물/생선요리 |
    | 베이커리/디저트 | 도넛, 떡/한과, 떡카페, 방앗간, 베이커리, 빙수, 스마일명품찹쌀꽈배기, 스마일찹쌀꽈배기, 와플, 케이크전문, 크레페, 호두과자, 호떡 |
    | 커피/음료 | 과일/주스전문점, 다방, 바나프레소, 아이스크림, 차, 카페, 카페/디저트, 테이크아웃커피 |
    | 브런치/샌드위치 | 브런치, 브런치카페, 샌드위치 |
    | 다이어트/샐러드 | 다이어트/샐러드, 채식/샐러드뷔페 |
    | 주점 | 단란주점, 라이브카페, 맥주/호프, 민속주점, 바(BAR), 술집, 와인, 요리주점, 이자카야, 전통/민속주점, 포장마차 |
    | 간편식 | 도시락/컵밥, 밀키트, 반찬가게 |
    | 기타 | 슈퍼/마트, 안경원, 야식, 음식점, 패밀리레스토랑, 푸드트럭, 퓨전음식, 향토음식 |

- db_menu
  - add a column of normalized menu name - use regex to
    - remove words indicating size : S, M, L, 대, 중, 소
    - remove words indicating set menu : 세트, set, Set, SET
    - remove price ranges: patterns like "7000-12000원", "5000~8000원"
    - remove quantity patterns: "2개", "500ml", etc.
    - leave only alphanumeric or korean words
  - add a column category_normalized to db_restaurant
    - store valid category (the key in VALID_CATEGORIES mapping) for efficient filtering

## client.py
### Given
- db_restaurant and db_menu
- user location : GEOMETRY(POINT, 4326)
- user profile
  - cuisine preference : korean, japanese, chinese, western, thai, italian, mexican, and indian
  - allergen : milk, eggs, peanuts, tree nuts, soy, wheat, fish, shellfish, and sesame
  - taste : spicy, sweet, and salty (in scale of 0 to 10)
### Goal
- provide restaurant recommendation system functionality
- (modularized to allow no range restriction) use PostGIS to filter only the restaurants within a certain range
- (modularized to allow no category restriction) filter only the restaurants of designated categories
  - supports both Korean category names and English aliases for category search
  - aliases for category search are as the following:
    | korean name(original) | english(alias) |
    |:---------------------:|:--------------:|
    | 한식 | korean |
    | 일식 | japanese |
    | 분식 | snackfood |
    | 중식 | chinese |
    | 양식 | western |
    | 세계음식 | global |
    | 패스트푸드 | fastfood |
    | 육류/고기요리 | meat |
    | 해산물 | seafood |
    | 베이커리/디저트 | bakery/dessert |
    | 커피/음료 | coffee/beverage |
    | 브런치/샌드위치 | brunch/sandwich |
    | 다이어트/샐러드 | healthy/salad |
    | 주점 | bar/pub |
    | 간편식 | convenience |
    | 기타 | miscellaneous |
- add user warning in case of no range or category restriction to confirm that the user wants to proceed the process despite latency
- categorize menus of the chosen restaurants using langchain
- find a good, simple combination of words representing the created category of menus using langchain
- recommends menu in a form of:
  - Menu category 1 : representing name
    - Reason of recommendation
    - Recommended menus
      - Menu 1 (restaurant name)
      - Menu 2 (restaurant name)
      - Menu 3 (restaurant name)
      ...
  - Menu category 2 : ...

## test.py
### Given
- client.py module (RestaurantRecommender class)
- db_restaurant and db_menu (via client.py)
- user location : GEOMETRY(POINT, 4326)
- user profile configurations for testing
### Goal
- provide comprehensive test suite for the restaurant recommendation system
- test database connectivity and data integrity
- test spatial queries and PostGIS functionality
- test restaurant filtering by distance and categories
- test menu categorization using LangChain
- test full recommendation pipeline
- test performance benchmarks
- validate that all functionality in client.py works correctly
- ensure system reliability and performance standards