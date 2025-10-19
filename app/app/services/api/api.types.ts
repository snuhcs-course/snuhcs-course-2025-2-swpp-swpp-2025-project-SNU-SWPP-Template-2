/**
 * These types indicate the shape of the data you expect to receive from your
 * API endpoint, assuming it's a JSON object like we have.
 */
export interface EpisodeItem {
  title: string
  pubDate: string
  link: string
  guid: string
  author: string
  thumbnail: string
  description: string
  content: string
  enclosure: {
    link: string
    type: string
    length: number
    duration: number
    rating: { scheme: string; value: string }
  }
  categories: string[]
}

export interface ApiFeedResponse {
  status: string
  feed: {
    url: string
    title: string
    link: string
    author: string
    description: string
    image: string
  }
  items: EpisodeItem[]
}

/**
 * 메뉴 추천 API 응답 타입
 */
export interface MenuRecommendationItem {
  id: number
  menu_name: string
  place_name: string
  price: number
  category: string
  location: string
  rating: number
  review_count: number
  keywords: string[]
  voted_keywords: string[]
  has_image: boolean
  image_urls: string[]  // 이미지 URL 배열 추가
  coordinates: [number, number]
  score: number
  reason: string
}

export interface MenuRecommendationResponse {
  success: boolean
  query_type: string
  total_results: number
  results: MenuRecommendationItem[]
}

/**
 * The options used to configure apisauce.
 */
export interface ApiConfig {
  /**
   * The URL of the api.
   */
  url: string

  /**
   * Milliseconds before we timeout the request.
   */
  timeout: number
}
