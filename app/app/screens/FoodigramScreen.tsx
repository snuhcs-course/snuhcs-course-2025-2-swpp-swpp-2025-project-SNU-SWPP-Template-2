import React, { useState, useEffect } from "react"
import { observer } from "mobx-react-lite"
import { View, ViewStyle, TextStyle, TouchableOpacity, ImageBackground } from "react-native"
import { Home, User, Bookmark, Heart } from "lucide-react-native"
import { Text } from "../components"
import { colors, spacing } from "../theme"
import { AppStackScreenProps } from "../navigators"
import { api } from "../services/api"
import type { MenuRecommendationItem } from "../services/api/api.types"
import { LinearGradient } from "expo-linear-gradient"

interface FoodigramScreenProps extends AppStackScreenProps<"Foodigram"> {}

export const FoodigramScreen: React.FC<FoodigramScreenProps> = observer(function FoodigramScreen({
  navigation,
}) {
  // 추천 메뉴 관련 상태
  const [recommendedMenus, setRecommendedMenus] = useState<MenuRecommendationItem[]>([])
  const [isLoadingRecommendations, setIsLoadingRecommendations] = useState(false)
  const [currentMenuIndex, setCurrentMenuIndex] = useState(0)
  const [bookmarkedItems, setBookmarkedItems] = useState<Set<number>>(new Set())
  const [favoritedItems, setFavoritedItems] = useState<Set<number>>(new Set())

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

  const handleNextMenu = () => {
    if (recommendedMenus.length === 0) return
    setCurrentMenuIndex((prev) => (prev + 1) % recommendedMenus.length)
  }

  const handlePreviousMenu = () => {
    if (recommendedMenus.length === 0) return
    setCurrentMenuIndex((prev) => (prev - 1 + recommendedMenus.length) % recommendedMenus.length)
  }

  const toggleBookmark = (menuId: number) => {
    setBookmarkedItems((prev) => {
      const newSet = new Set(prev)
      if (newSet.has(menuId)) {
        newSet.delete(menuId)
      } else {
        newSet.add(menuId)
      }
      return newSet
    })
  }

  const toggleFavorite = (menuId: number) => {
    setFavoritedItems((prev) => {
      const newSet = new Set(prev)
      if (newSet.has(menuId)) {
        newSet.delete(menuId)
      } else {
        newSet.add(menuId)
      }
      return newSet
    })
  }

  const currentMenu = recommendedMenus[currentMenuIndex]

  return (
    <View style={$container}>
      {/* Main Full Screen Content */}
      <View style={$fullScreenContainer}>
        {isLoadingRecommendations ? (
          // 로딩 상태
          <View style={$loadingContainer}>
            <Text style={$loadingText}>추천 메뉴를 가져오는 중...</Text>
          </View>
        ) : recommendedMenus.length > 0 && currentMenu ? (
          // 전체 화면 메뉴 표시
          <>
            {/* Background Image */}
            <ImageBackground
              source={{
                uri:
                  currentMenu.image_urls && currentMenu.image_urls.length > 0
                    ? currentMenu.image_urls[0]
                    : undefined,
              }}
              style={$backgroundImage}
              resizeMode="cover"
            >
              {/* Gradient Overlay */}
              <LinearGradient
                colors={["transparent", "rgba(0,0,0,0.8)"]}
                style={$gradientOverlay}
              />

              {/* Top Header */}
              <View style={$topHeader}>
                <Text style={$discoverTitle}>Discover</Text>
                <View style={$headerButtons}>
                  <TouchableOpacity
                    style={$headerButton}
                    onPress={() => toggleBookmark(currentMenu.id)}
                  >
                    <Bookmark
                      size={24}
                      color="#fff"
                      fill={bookmarkedItems.has(currentMenu.id) ? "#fff" : "transparent"}
                    />
                  </TouchableOpacity>
                  <TouchableOpacity
                    style={$headerButton}
                    onPress={() => toggleFavorite(currentMenu.id)}
                  >
                    <Heart
                      size={24}
                      color={favoritedItems.has(currentMenu.id) ? "#f66c51" : "#fff"}
                      fill={favoritedItems.has(currentMenu.id) ? "#f66c51" : "transparent"}
                    />
                  </TouchableOpacity>
                </View>
              </View>

              {/* Bottom Info */}
              <View style={$bottomInfo}>
                <Text style={$restaurantName}>{currentMenu.place_name}</Text>
                <Text style={$restaurantDetails}>
                  {currentMenu.category} • {currentMenu.location}
                </Text>
                <View style={$menuDetails}>
                  <Text style={$menuNameLarge}>{currentMenu.menu_name}</Text>
                  <Text style={$menuPriceLarge}>
                    {currentMenu.price?.toLocaleString()}원
                  </Text>
                  <Text style={$menuRatingLarge}>
                    ⭐ {currentMenu.rating} ({currentMenu.review_count} 리뷰)
                  </Text>
                  {currentMenu.reason && (
                    <Text style={$menuReasonLarge}>{currentMenu.reason}</Text>
                  )}
                </View>
              </View>

              {/* Touch Areas for Navigation */}
              <TouchableOpacity
                style={$leftTouchArea}
                activeOpacity={1}
                onPress={handlePreviousMenu}
              />
              <TouchableOpacity
                style={$rightTouchArea}
                activeOpacity={1}
                onPress={handleNextMenu}
              />
            </ImageBackground>
          </>
        ) : (
          // 에러 상태
          <View style={$emptyState}>
            <Text style={$emptyText}>추천 메뉴를 불러올 수 없습니다</Text>
            <Text style={$emptySubtext}>잠시 후 다시 시도해주세요</Text>
          </View>
        )}
      </View>

      {/* Bottom Tabs */}
      <View style={$bottomTabs}>
        <TouchableOpacity
          style={[$tabButton, $tabButtonActive]}
          onPress={() => {
            if (__DEV__) {
              console.log(`Home: tapped home button (already on Foodigram)`)
            }
            navigation.navigate("Foodigram")
          }}
        >
          <Home size={28} color={colors.palette.primary500} />
          <Text style={$tabButtonTextActive}>Discover</Text>
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
          <Text style={$tabButtonText}>Profile</Text>
        </TouchableOpacity>
      </View>

    </View>
  )
})

const $container: ViewStyle = {
  flex: 1,
  backgroundColor: colors.background,
}

const $fullScreenContainer: ViewStyle = {
  flex: 1,
  position: "relative",
}

const $backgroundImage: ViewStyle = {
  flex: 1,
  width: "100%",
  height: "100%",
}

const $gradientOverlay: ViewStyle = {
  position: "absolute",
  left: 0,
  right: 0,
  bottom: 0,
  height: "50%",
}

const $topHeader: ViewStyle = {
  position: "absolute",
  top: 0,
  left: 0,
  right: 0,
  paddingTop: spacing.xxl + spacing.md,
  paddingHorizontal: spacing.md,
  paddingBottom: spacing.md,
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
  zIndex: 10,
}

const $discoverTitle: TextStyle = {
  fontSize: 28,
  fontWeight: "bold",
  color: "#fff",
}

const $headerButtons: ViewStyle = {
  flexDirection: "row",
  gap: spacing.sm,
}

const $headerButton: ViewStyle = {
  width: 44,
  height: 44,
  borderRadius: 22,
  backgroundColor: "rgba(0,0,0,0.4)",
  justifyContent: "center",
  alignItems: "center",
}

const $bottomInfo: ViewStyle = {
  position: "absolute",
  bottom: 65, // Above bottom tabs
  left: 0,
  right: 0,
  paddingHorizontal: spacing.lg,
  paddingBottom: spacing.lg,
  zIndex: 10,
}

const $restaurantName: TextStyle = {
  fontSize: 32,
  fontWeight: "bold",
  color: "#fff",
  marginBottom: spacing.xs,
}

const $restaurantDetails: TextStyle = {
  fontSize: 20,
  color: "#fff",
  marginBottom: spacing.md,
}

const $menuDetails: ViewStyle = {
  gap: spacing.xs,
}

const $menuNameLarge: TextStyle = {
  fontSize: 18,
  fontWeight: "600",
  color: "#fff",
}

const $menuPriceLarge: TextStyle = {
  fontSize: 16,
  fontWeight: "600",
  color: "#fff",
}

const $menuRatingLarge: TextStyle = {
  fontSize: 14,
  color: "#fff",
}

const $menuReasonLarge: TextStyle = {
  fontSize: 14,
  color: "rgba(255,255,255,0.8)",
  fontStyle: "italic",
  marginTop: spacing.xs,
}

const $leftTouchArea: ViewStyle = {
  position: "absolute",
  top: 0,
  left: 0,
  width: "50%",
  height: "100%",
  zIndex: 5,
}

const $rightTouchArea: ViewStyle = {
  position: "absolute",
  top: 0,
  right: 0,
  width: "50%",
  height: "100%",
  zIndex: 5,
}

const $loadingContainer: ViewStyle = {
  flex: 1,
  justifyContent: "center",
  alignItems: "center",
  backgroundColor: colors.background,
}

const $loadingText: TextStyle = {
  fontSize: 16,
  color: colors.text,
}

const $emptyState: ViewStyle = {
  flex: 1,
  justifyContent: "center",
  alignItems: "center",
  paddingHorizontal: spacing.md,
  backgroundColor: colors.background,
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

const $bottomTabs: ViewStyle = {
  position: "absolute",
  bottom: 0,
  left: 0,
  right: 0,
  height: 65,
  flexDirection: "row",
  borderTopWidth: 1,
  borderTopColor: colors.palette.neutral200,
  backgroundColor: colors.background,
  zIndex: 100,
}

const $tabButton: ViewStyle = {
  flex: 1,
  height: "100%",
  alignItems: "center",
  justifyContent: "center",
  paddingVertical: spacing.xs,
}

const $tabButtonActive: ViewStyle = {
  backgroundColor: colors.palette.primary100,
}

const $tabButtonText: TextStyle = {
  fontSize: 12,
  color: colors.palette.neutral500,
  marginTop: 2,
}

const $tabButtonTextActive: TextStyle = {
  fontSize: 12,
  color: colors.palette.primary500,
  marginTop: 2,
}


