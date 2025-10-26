import React, { useState, useEffect } from "react"
import { observer } from "mobx-react-lite"
import {
  View,
  ViewStyle,
  TextStyle,
  TouchableOpacity,
  FlatList,
  Dimensions,
  Image,
} from "react-native"
import { Filter, Users, Home, User } from "lucide-react-native"
import { Text } from "../components"
import { colors, spacing } from "../theme"
import { friends, allCategories, allAllergens } from "../data/mockData"
import { AppStackScreenProps } from "../navigators"
import { useStores } from "../models"
import { api } from "../services/api"
import type { MenuRecommendationItem } from "../services/api/api.types"

interface FoodigramScreenProps extends AppStackScreenProps<"Foodigram"> {}

export const FoodigramScreen: React.FC<FoodigramScreenProps> = observer(function FoodigramScreen({
  navigation,
}) {
  const { foodHistoryStore } = useStores()
  const [isFilterOpen, setIsFilterOpen] = useState(false)
  const [isFriendsOpen, setIsFriendsOpen] = useState(false)
  const [selectedCategories, setSelectedCategories] = useState<string[]>(allCategories)
  const [selectedAllergens, setSelectedAllergens] = useState<string[]>([])
  const [screenData, setScreenData] = useState(Dimensions.get("window"))

  // 추천 메뉴 관련 상태
  const [recommendedMenus, setRecommendedMenus] = useState<MenuRecommendationItem[]>([])
  const [isLoadingRecommendations, setIsLoadingRecommendations] = useState(false)

  // Listen for screen dimension changes
  useEffect(() => {
    const subscription = Dimensions.addEventListener("change", ({ window }) => {
      setScreenData(window)
    })

    return () => subscription?.remove()
  }, [])

  // Load scraps from server on mount
  useEffect(() => {
    const loadScrapsFromServer = async () => {
      try {
        const response = await api.getScraps()

        if (response.ok && response.data) {
          const serverScraps = response.data as any[]

          // 서버에서 가져온 스크랩된 restaurant ID 목록
          const scrapedRestaurantIds = serverScraps
            .map((scrap: any) => scrap.restaurant?.id)
            .filter(Boolean)

          // 로컬 store 초기화: 현재 로컬에 있는 것 중 서버에 없는 것은 제거
          const currentLocalScraps = foodHistoryStore.scrappedItems.map((item) => item.id)

          // 서버에는 있는데 로컬에는 없는 것 추가
          scrapedRestaurantIds.forEach((restaurantId: number) => {
            const foodItem = foodItems.find((f) => f.id === restaurantId)
            if (foodItem && !currentLocalScraps.includes(restaurantId)) {
              foodHistoryStore.toggleScrappedItem(foodItem)
            }
          })

          // 로컬에는 있는데 서버에는 없는 것 제거
          currentLocalScraps.forEach((localId: number) => {
            if (!scrapedRestaurantIds.includes(localId)) {
              const foodItem = foodItems.find((f) => f.id === localId)
              if (foodItem) {
                foodHistoryStore.toggleScrappedItem(foodItem)
              }
            }
          })

          if (__DEV__) {
            console.log(`Home: Synced scraps from server. Total: ${scrapedRestaurantIds.length}`)
          }
        }
      } catch (error) {
        if (__DEV__) {
          console.error("Home: Failed to load scraps from server:", error)
        }
      }
    }

    loadScrapsFromServer()
  }, [])

  // 화면 로드 시 자동으로 추천 메뉴 가져오기
  useEffect(() => {
    const fetchInitialRecommendations = async () => {
      setIsLoadingRecommendations(true)
      try {
        // 사용자 위치 정보 (실제로는 GPS나 사용자 설정에서 가져와야 함)
        const userLocation: [number, number] = [126.9619864, 37.477136] // 서울 강남역 좌표

        const recommendations = await api.getMenuRecommendations(userLocation, {
          maxResults: 10,
        })

        if (recommendations && recommendations.success && recommendations.results) {
          setRecommendedMenus(recommendations.results)
        }
      } catch (error) {
        console.error("추천 메뉴 가져오기 실패:", error)
      } finally {
        setIsLoadingRecommendations(false)
      }
    }

    fetchInitialRecommendations()
  }, [])

  const handleCategoryToggle = (category: string) => {
    const isCurrentlySelected = selectedCategories.includes(category)
    const action = isCurrentlySelected ? "deselected" : "selected"

    if (__DEV__) {
      console.log(`Home: ${action} category filter "${category}"`)
    }

    setSelectedCategories((prev) =>
      prev.includes(category) ? prev.filter((c) => c !== category) : [...prev, category],
    )
  }

  const handleAllergenToggle = (allergen: string) => {
    const isCurrentlySelected = selectedAllergens.includes(allergen)
    const action = isCurrentlySelected ? "removed" : "added"

    if (__DEV__) {
      console.log(`Home: ${action} allergen filter "${allergen}"`)
    }

    setSelectedAllergens((prev) =>
      prev.includes(allergen) ? prev.filter((a) => a !== allergen) : [...prev, allergen],
    )
  }

  return (
    <View style={$container}>
      {/* Header */}
      <View style={$header}>
        <Text style={$headerTitle}>foodigram</Text>
      </View>

      {/* Main Content - Takes remaining space */}
      <View style={$mainContent}>
        {isLoadingRecommendations ? (
          // 로딩 스피너 표시
          <View style={$loadingContainer}>
            <Text style={$loadingText}>추천 메뉴를 가져오는 중...</Text>
          </View>
        ) : recommendedMenus.length > 0 ? (
          // 추천 메뉴 카드 목록 표시
          <View style={$recommendationsContainer}>
            <Text style={$recommendationsTitle}>추천 메뉴</Text>
            <FlatList
              data={recommendedMenus}
              keyExtractor={(item) => item.id.toString()}
              horizontal
              showsHorizontalScrollIndicator={false}
              contentContainerStyle={$recommendationsList}
              renderItem={({ item }) => (
                <View style={$recommendationCard}>
                  {item.image_urls && item.image_urls.length > 0 ? (
                    <Image
                      source={{ uri: item.image_urls[0] }}
                      style={$menuImage}
                      resizeMode="cover"
                    />
                  ) : (
                    <View style={$menuImagePlaceholder}>
                      <Text style={$menuImagePlaceholderText}>이미지 없음</Text>
                    </View>
                  )}
                  <View style={$menuInfo}>
                    <Text style={$menuName}>{item.menu_name}</Text>
                    <Text style={$placeName}>{item.place_name}</Text>
                    <Text style={$menuPrice}>{item.price?.toLocaleString()}원</Text>
                    <Text style={$menuCategory}>{item.category}</Text>
                    <Text style={$menuLocation}>{item.location}</Text>
                    <Text style={$menuRating}>
                      ⭐ {item.rating} ({item.review_count}리뷰)
                    </Text>
                    <Text style={$menuReason}>{item.reason}</Text>
                  </View>
                </View>
              )}
            />
          </View>
        ) : (
          // 에러 상태
          <View style={$emptyState}>
            <Text style={$emptyText}>추천 메뉴를 불러올 수 없습니다</Text>
            <Text style={$emptySubtext}>잠시 후 다시 시도해주세요</Text>
          </View>
        )}
      </View>

      {/* Action Buttons */}
      <View style={$actionButtons}>
        <TouchableOpacity
          style={[
            $actionButton,
            isFilterOpen && $actionButtonHighlighted,
            (isFilterOpen || isFriendsOpen) && !isFilterOpen && $actionButtonDimmed,
          ]}
          onPress={() => {
            if (__DEV__) {
              console.log(`Home: opened filter panel`)
            }
            setIsFilterOpen(true)
          }}
        >
          <Filter size={16} color={colors.palette.neutral700} />
          <Text style={$actionButtonText}>Filter</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[
            $actionButton,
            isFriendsOpen && $actionButtonHighlighted,
            (isFilterOpen || isFriendsOpen) && !isFriendsOpen && $actionButtonDimmed,
          ]}
          onPress={() => {
            if (__DEV__) {
              console.log(`Home: opened friends panel`)
            }
            setIsFriendsOpen(true)
          }}
        >
          <Users size={16} color={colors.palette.neutral700} />
          <Text style={$actionButtonText}>Friends</Text>
        </TouchableOpacity>
      </View>

      {/* Bottom Tabs */}
      <View style={$bottomTabs}>
        <TouchableOpacity
          style={[$tabButton, $tabButtonActive]}
          onPress={() => {
            if (__DEV__) {
              console.log(`Home: tapped home button (already on Foodigram)`)
            }
            // already on Recommendation (Foodigram)
            navigation.navigate("Foodigram")
          }}
        >
          <Home size={28} color={colors.palette.primary500} />
        </TouchableOpacity>

        <TouchableOpacity
          style={$tabButton}
          onPress={() => {
            if (__DEV__) {
              console.log(`Home: navigated to Profile`)
            }
            navigation.navigate("Profile")
          }}
        >
          <User size={28} color={colors.palette.neutral500} />
        </TouchableOpacity>
      </View>

      {/* Filter Speech Bubble */}
      {isFilterOpen && (
        <View style={$speechBubbleContainer}>
          <TouchableOpacity
            style={$speechBubbleBackdrop}
            activeOpacity={1}
            onPress={() => {
              if (__DEV__) {
                console.log(`Home: closed filter panel`)
              }
              setIsFilterOpen(false)
            }}
          />
          <View style={$speechBubble}>
            <View style={$speechBubbleHeader}>
              <Text style={$speechBubbleTitle}>Filters</Text>
              <TouchableOpacity
                onPress={() => {
                  if (__DEV__) {
                    console.log(`Home: closed filter panel (X button)`)
                  }
                  setIsFilterOpen(false)
                }}
              >
                <X size={20} color={colors.palette.neutral700} />
              </TouchableOpacity>
            </View>

            <ScrollView
              style={$speechBubbleContent}
              showsVerticalScrollIndicator={true}
              scrollEnabled={true}
              nestedScrollEnabled={true}
            >
              <Text style={$sectionTitle}>Categories</Text>
              <View style={$filterGrid}>
                {allCategories.map((category) => (
                  <TouchableOpacity
                    key={category}
                    style={[
                      $filterChip,
                      selectedCategories.includes(category) && $filterChipSelected,
                    ]}
                    onPress={() => handleCategoryToggle(category)}
                  >
                    <Text
                      style={[
                        $filterChipText,
                        selectedCategories.includes(category) && $filterChipTextSelected,
                      ]}
                    >
                      {category}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>

              <Text style={$sectionTitle}>Exclude Allergens</Text>
              <View style={$filterGrid}>
                {allAllergens.map((allergen) => (
                  <TouchableOpacity
                    key={allergen}
                    style={[
                      $filterChip,
                      selectedAllergens.includes(allergen) && $filterChipSelected,
                    ]}
                    onPress={() => handleAllergenToggle(allergen)}
                  >
                    <Text
                      style={[
                        $filterChipText,
                        selectedAllergens.includes(allergen) && $filterChipTextSelected,
                      ]}
                    >
                      {allergen}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </ScrollView>
          </View>
        </View>
      )}

      {/* Friends Speech Bubble */}
      {isFriendsOpen && (
        <View style={$speechBubbleContainer}>
          <TouchableOpacity
            style={$speechBubbleBackdrop}
            activeOpacity={1}
            onPress={() => {
              if (__DEV__) {
                console.log(`Home: closed friends panel`)
              }
              setIsFriendsOpen(false)
            }}
          />
          <View style={$speechBubble}>
            <View style={$speechBubbleHeader}>
              <Text style={$speechBubbleTitle}>Friends</Text>
              <TouchableOpacity
                onPress={() => {
                  if (__DEV__) {
                    console.log(`Home: closed friends panel (X button)`)
                  }
                  setIsFriendsOpen(false)
                }}
              >
                <X size={20} color={colors.palette.neutral700} />
              </TouchableOpacity>
            </View>

            <FlatList
              data={friends}
              keyExtractor={(item) => item.id.toString()}
              renderItem={({ item }) => (
                <View style={$friendItem}>
                  <Text style={$friendName}>{item.name}</Text>
                  <Text style={$friendLikes}>{item.mutualLikes.length} mutual likes</Text>
                </View>
              )}
              style={$speechBubbleContent}
              showsVerticalScrollIndicator={true}
              scrollEnabled={true}
              nestedScrollEnabled={true}
            />
          </View>
        </View>
      )}
    </View>
  )
})

const $container: ViewStyle = {
  flex: 1,
  backgroundColor: colors.background,
  position: "relative",
}

const $header: ViewStyle = {
  position: "absolute",
  top: 0,
  left: 0,
  right: 0,
  paddingHorizontal: spacing.md,
  paddingTop: spacing.md,
  paddingBottom: spacing.sm,
  borderBottomWidth: 1,
  borderBottomColor: colors.palette.neutral200,
  backgroundColor: colors.background,
  zIndex: 1000,
}

const $headerTitle: TextStyle = {
  fontSize: 24,
  fontWeight: "bold",
  textAlign: "center",
  marginTop: spacing.xxl,
  marginBottom: spacing.md,
  color: colors.text,
}

const $mainContent: ViewStyle = {
  position: "absolute",
  top: 60, // Header height
  left: 0,
  right: 0,
  bottom: 125, // Action buttons (60) + bottom tabs (65)
  justifyContent: "center",
  alignItems: "center",
  zIndex: 1,
}

const $emptyState: ViewStyle = {
  alignItems: "center",
  paddingHorizontal: spacing.md,
}

const $emptyText: TextStyle = {
  fontSize: 18,
  color: colors.palette.neutral400,
  marginBottom: spacing.xs,
}

const $emptySubtext: TextStyle = {
  fontSize: 14,
  color: colors.palette.neutral400,
  textAlign: "center",
}

const $actionButtons: ViewStyle = {
  position: "absolute",
  bottom: 65, // Above bottom tabs
  left: 0,
  right: 0,
  flexDirection: "row",
  justifyContent: "space-around",
  paddingHorizontal: spacing.md,
  paddingVertical: spacing.md,
  borderTopWidth: 1,
  borderTopColor: colors.palette.neutral200,
  backgroundColor: colors.background,
  zIndex: 100,
}

const $actionButton: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  paddingHorizontal: spacing.md,
  paddingVertical: spacing.xs,
  borderRadius: 20,
  gap: spacing.xs,
  zIndex: 3000, // Higher than speech bubble (2000)
}

const $actionButtonActive: ViewStyle = {
  backgroundColor: colors.palette.primary500,
}

const $actionButtonText: TextStyle = {
  fontSize: 14,
  color: colors.palette.neutral700,
}

const $bottomTabs: ViewStyle = {
  position: "absolute",
  bottom: 0,
  left: 0,
  right: 0,
  height: 65, // Increased height to accommodate larger icons and text
  flexDirection: "row",
  borderTopWidth: 1,
  borderTopColor: colors.palette.neutral200,
  backgroundColor: colors.background,
  zIndex: 100,
}

const $tabButton: ViewStyle = {
  flex: 1,
  height: "100%", // Take full height of parent
  alignItems: "center",
  justifyContent: "center", // Center the icon
  paddingVertical: spacing.md,
}

const $tabButtonActive: ViewStyle = {
  backgroundColor: colors.palette.primary100,
}

const $speechBubbleContainer: ViewStyle = {
  position: "absolute",
  top: 0,
  bottom: 0,
  left: 0,
  right: 0,
  justifyContent: "center",
  alignItems: "center",
  paddingHorizontal: spacing.md,
  zIndex: 2000, // High z-index to appear above all other elements
}

const $speechBubbleBackdrop: ViewStyle = {
  position: "absolute",
  top: 0,
  bottom: 0,
  left: 0,
  right: 0,
  backgroundColor: "rgba(0, 0, 0, 0.4)", // Semi-transparent overlay covering full area
}

const $speechBubble: ViewStyle = {
  backgroundColor: colors.background,
  borderRadius: 16,
  width: "85%", // Fixed width for consistent sizing
  maxHeight: "90%",
  minHeight: "60%",
  shadowColor: "#000",
  shadowOffset: {
    width: 0,
    height: 4,
  },
  shadowOpacity: 0.25,
  shadowRadius: 8,
  elevation: 8,
}

const $speechBubbleHeader: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
  paddingHorizontal: spacing.md,
  paddingVertical: spacing.sm,
  borderBottomWidth: 1,
  borderBottomColor: colors.palette.neutral400,
}

const $speechBubbleTitle: TextStyle = {
  fontSize: 16,
  fontWeight: "bold",
  color: colors.text,
}

const $speechBubbleContent: ViewStyle = {
  flex: 1,
  paddingHorizontal: spacing.md,
}

const $sectionTitle: TextStyle = {
  fontSize: 16,
  fontWeight: "bold",
  marginTop: spacing.lg,
  marginBottom: spacing.sm,
  color: colors.text,
}

const $filterGrid: ViewStyle = {
  flexDirection: "row",
  flexWrap: "wrap",
  gap: spacing.xs,
  marginBottom: spacing.md,
}

const $filterChip: ViewStyle = {
  paddingHorizontal: spacing.sm,
  paddingVertical: spacing.xs,
  borderRadius: 16,
  borderWidth: 1,
  borderColor: colors.palette.neutral300,
  backgroundColor: colors.background,
}

const $filterChipSelected: ViewStyle = {
  backgroundColor: colors.palette.primary500,
  borderColor: colors.palette.primary500,
}

const $filterChipText: TextStyle = {
  fontSize: 12,
  color: colors.palette.neutral700,
  textTransform: "capitalize",
}

const $filterChipTextSelected: TextStyle = {
  color: colors.palette.neutral100,
}

const $friendItem: ViewStyle = {
  paddingVertical: spacing.md,
  borderBottomWidth: 1,
  borderBottomColor: colors.palette.neutral200,
}

const $friendName: TextStyle = {
  fontSize: 16,
  fontWeight: "bold",
  color: colors.text,
  marginBottom: spacing.xs,
}

const $friendLikes: TextStyle = {
  fontSize: 14,
  color: colors.palette.neutral500,
}

const $actionButtonHighlighted: ViewStyle = {
  backgroundColor: colors.background, // Keep clicked button bright and undarkened
  shadowColor: "#000",
  shadowOffset: {
    width: 0,
    height: 2,
  },
  shadowOpacity: 0.15,
  shadowRadius: 4,
  elevation: 4,
}

const $actionButtonDimmed: ViewStyle = {
  backgroundColor: "rgba(0, 0, 0, 0.4)", // Apply same darkening as other components
}

// 로딩 관련 스타일
const $loadingContainer: ViewStyle = {
  flex: 1,
  justifyContent: "center",
  alignItems: "center",
}

const $loadingText: TextStyle = {
  fontSize: 16,
  color: colors.palette.neutral600,
  textAlign: "center",
}

// 추천 메뉴 관련 스타일
const $recommendationsContainer: ViewStyle = {
  flex: 1,
  backgroundColor: colors.background,
  paddingHorizontal: spacing.md,
}

const $recommendationsTitle: TextStyle = {
  fontSize: 20,
  fontWeight: "bold",
  color: colors.text,
  marginBottom: spacing.md,
  textAlign: "center",
}

const $recommendationsList: ViewStyle = {
  paddingHorizontal: spacing.sm,
}

const $recommendationCard: ViewStyle = {
  width: 280,
  backgroundColor: colors.palette.neutral100,
  borderRadius: 16,
  marginRight: spacing.md,
  shadowColor: "#000",
  shadowOffset: {
    width: 0,
    height: 2,
  },
  shadowOpacity: 0.1,
  shadowRadius: 4,
  elevation: 3,
}

const $menuImage: ViewStyle = {
  width: "100%",
  height: 180,
  borderTopLeftRadius: 16,
  borderTopRightRadius: 16,
}

const $menuImagePlaceholder: ViewStyle = {
  width: "100%",
  height: 180,
  backgroundColor: colors.palette.neutral200,
  borderTopLeftRadius: 16,
  borderTopRightRadius: 16,
  justifyContent: "center",
  alignItems: "center",
}

const $menuImagePlaceholderText: TextStyle = {
  fontSize: 14,
  color: colors.palette.neutral500,
}

const $menuInfo: ViewStyle = {
  padding: spacing.md,
}

const $recommendationItem: ViewStyle = {
  backgroundColor: colors.palette.neutral100,
  padding: spacing.md,
  marginVertical: spacing.sm,
  borderRadius: 12,
  borderWidth: 1,
  borderColor: colors.palette.neutral200,
}

const $menuName: TextStyle = {
  fontSize: 16,
  fontWeight: "bold",
  color: colors.text,
  marginBottom: spacing.xs,
}

const $placeName: TextStyle = {
  fontSize: 14,
  color: colors.palette.neutral700,
  marginBottom: spacing.xs,
}

const $menuPrice: TextStyle = {
  fontSize: 14,
  fontWeight: "600",
  color: colors.palette.primary500,
  marginBottom: spacing.xs,
}

const $menuCategory: TextStyle = {
  fontSize: 12,
  color: colors.palette.neutral600,
  marginBottom: spacing.xs,
}

const $menuLocation: TextStyle = {
  fontSize: 12,
  color: colors.palette.neutral600,
  marginBottom: spacing.xs,
}

const $menuRating: TextStyle = {
  fontSize: 12,
  color: colors.palette.neutral600,
  marginBottom: spacing.xs,
}

const $menuReason: TextStyle = {
  fontSize: 12,
  color: colors.palette.neutral500,
  fontStyle: "italic",
}
