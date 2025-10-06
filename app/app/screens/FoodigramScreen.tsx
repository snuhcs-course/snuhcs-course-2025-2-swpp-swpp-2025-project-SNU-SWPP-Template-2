import React, { useState, useEffect, useRef } from "react"
import { observer } from "mobx-react-lite"
import {
  View,
  ViewStyle,
  TextStyle,
  TouchableOpacity,
  ScrollView,
  FlatList,
  Dimensions,
} from "react-native"
import { PanGestureHandler, NativeViewGestureHandler } from "react-native-gesture-handler"
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  useAnimatedGestureHandler,
  withSpring,
  runOnJS,
} from "react-native-reanimated"
import { Search, Filter, Heart, Users, Home, User, X } from "lucide-react-native"
import { Text, TextField } from "../components"
import { FoodCard } from "../components/FoodCard"
import { colors, spacing } from "../theme"
import { foodItems, friends, allCategories, allAllergens } from "../data/mockData"
import { FoodItem } from "../types/FoodTypes"
import { AppStackScreenProps } from "../navigators"
import { useStores } from "../models"

interface FoodigramScreenProps extends AppStackScreenProps<"Foodigram"> {}

export const FoodigramScreen: React.FC<FoodigramScreenProps> = observer(function FoodigramScreen({ navigation }) {
  const { foodHistoryStore } = useStores()
  const [searchQuery, setSearchQuery] = useState("")
  const [currentIndex, setCurrentIndex] = useState(0)
  const [likedItems, setLikedItems] = useState<number[]>([])
  const [showLikedOnly, setShowLikedOnly] = useState(false)
  const [isFilterOpen, setIsFilterOpen] = useState(false)
  const [isFriendsOpen, setIsFriendsOpen] = useState(false)
  const [selectedCategories, setSelectedCategories] = useState<string[]>(allCategories)
  const [selectedAllergens, setSelectedAllergens] = useState<string[]>([])
  const [screenData, setScreenData] = useState(Dimensions.get('window'))
  
  // Create refs for scroll views to handle gesture conflicts
  const panGestureRef = useRef(null)
  const titleScrollGestureRef = useRef(null)
  const allergenScrollGestureRef = useRef(null)

  // Filter foods based on search, filters, and liked view
  const getFilteredFoods = (): FoodItem[] => {
    let filtered = [...foodItems]

    // Search filter
    if (searchQuery.trim()) {
      filtered = filtered.filter(
        (food) =>
          food.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
          food.keywords.some((k) => k.toLowerCase().includes(searchQuery.toLowerCase()))
      )
    }

    // Category filter
    if (selectedCategories.length > 0) {
      filtered = filtered.filter((food) => selectedCategories.includes(food.category))
    }

    // Allergen filter (exclude foods with selected allergens)
    if (selectedAllergens.length > 0) {
      filtered = filtered.filter(
        (food) => !food.allergens.some((allergen) => selectedAllergens.includes(allergen))
      )
    }

    // Liked filter
    if (showLikedOnly) {
      filtered = filtered.filter((food) => likedItems.includes(food.id))
    }

    return filtered
  }

  const filteredFoods = getFilteredFoods()

  // Listen for screen dimension changes
  useEffect(() => {
    const subscription = Dimensions.addEventListener('change', ({ window }) => {
      setScreenData(window)
    })
    
    return () => subscription?.remove()
  }, [])

  // Animated values for swipe
  const translateX = useSharedValue(0)
  const scale = useSharedValue(1)
  const startPosition = useSharedValue(0)
  const isGestureActive = useSharedValue(false)

  // Reset index when filters change
  useEffect(() => {
    if (currentIndex >= filteredFoods.length && filteredFoods.length > 0) {
      setCurrentIndex(0)
    }
  }, [filteredFoods.length, currentIndex])

  // Reset animation values when food card changes
  useEffect(() => {
    translateX.value = 0
    scale.value = 1
  }, [currentIndex, translateX, scale])

  const currentFood = filteredFoods[currentIndex]

  // Calculate dynamic scaling for middle section only
  const getDynamicStyles = () => {
    const { height, width } = screenData
    
    // Fixed element heights (these don't scale)
    const headerHeight = 120 // Header + search bar
    const actionButtonsHeight = 60 // Action buttons (fixed size)
    const bottomTabsHeight = 65 // Bottom tabs (fixed size)
    const counterHeight = 30 // Food counter indicator
    const middleSectionMargins = spacing.md * 2 // Reduced margins due to constrained space
    
    // Calculate available space for middle section (food card + counter)
    // With absolute positioning: mainContent has top: 120, bottom: 125
    const availableMiddleHeight = height - 120 - 125 - middleSectionMargins
    const availableWidth = width - (spacing.md * 2) // Side margins
    
    // Define ideal dimensions for middle section
    const idealFoodCardHeight = 520
    const idealFoodCardWidth = width - (spacing.lg * 2) // Full screen width minus margins
    const idealMiddleSectionHeight = idealFoodCardHeight + counterHeight + spacing.md // food card + counter + margin between them
    
    // Calculate scaling factor for middle section only
    const heightScale = Math.min(1, availableMiddleHeight / idealMiddleSectionHeight)
    const widthScale = Math.min(1, availableWidth / idealFoodCardWidth)
    const scale = Math.min(heightScale, widthScale, 1) // Don't scale up, only down
    
    return {
      foodCardScale: scale,
      foodCardHeight: idealFoodCardHeight * scale,
      foodCardWidth: idealFoodCardWidth * scale,
    }
  }

  const dynamicStyles = getDynamicStyles()

  const handleLike = () => {
    if (!currentFood) return
    setLikedItems((prev) =>
      prev.includes(currentFood.id)
        ? prev.filter((id) => id !== currentFood.id)
        : [...prev, currentFood.id]
    )
  }

  const handleScrap = () => {
    if (!currentFood) return
    foodHistoryStore.toggleScrappedItem(currentFood)
  }

  const handleNext = () => {
    if (currentIndex < filteredFoods.length - 1) {
      setCurrentIndex(currentIndex + 1)
    }
  }

  const handlePrev = () => {
    if (currentIndex > 0) {
      setCurrentIndex(currentIndex - 1)
    }
  }

  // Calculate card width for carousel effect - use screen width to center cards
  const cardWidth = screenData.width // Full screen width for centering
  
  // Animated gesture handler for carousel
  const gestureHandler = useAnimatedGestureHandler({
    onStart: () => {
      // Store the starting position to avoid jumps and mark gesture as active
      startPosition.value = translateX.value
      isGestureActive.value = true
    },
    onActive: (event) => {
      // Move from the start position based on finger movement
      translateX.value = startPosition.value + event.translationX
    },
    onEnd: (event) => {
      const targetIndex = currentIndex
      let newIndex = targetIndex
      
      // Determine new index based on swipe - more sensitive for single card movement
      if (event.translationX < -30 && event.velocityX < -200) {
        // Swipe left - go to next
        newIndex = Math.min(targetIndex + 1, filteredFoods.length - 1)
      } else if (event.translationX > 30 && event.velocityX > 200) {
        // Swipe right - go to previous
        newIndex = Math.max(targetIndex - 1, 0)
      }
      
      // If index changed, animate from release position to adjacent card
      if (newIndex !== targetIndex) {
        // Calculate target position in CURRENT layout (before index change)
        // Current layout positions: [prev, current, next]
        const currentPosition = targetIndex > 0 ? -cardWidth : 0
        
        let targetPosition
        if (newIndex > targetIndex) {
          // Swiping left to next card: animate to next card position (right of current)
          targetPosition = currentPosition - cardWidth
        } else {
          // Swiping right to previous card: animate to previous card position (left of current)  
          targetPosition = currentPosition + cardWidth
        }
        
        // Animate from release position to adjacent card position
        translateX.value = withSpring(targetPosition, { 
          damping: 20,
          stiffness: 300 
        }, () => {
          // Calculate the correct final position for the new layout
          const finalPosition = newIndex > 0 ? -cardWidth : 0
          
          // Set final position directly (this happens before index update)
          translateX.value = finalPosition
          
          // Update index last
          runOnJS(setCurrentIndex)(newIndex)
          
          // Mark gesture as finished
          isGestureActive.value = false
        })
      } else {
        // If no index change, snap back to current position from release point
        const currentPosition = targetIndex > 0 ? -cardWidth : 0
        translateX.value = withSpring(currentPosition, { 
          damping: 20,
          stiffness: 300 
        }, () => {
          isGestureActive.value = false
        })
      }
    },
  })

  // Animated style for the carousel container
  const animatedCarouselStyle = useAnimatedStyle(() => {
    return {
      transform: [
        { translateX: translateX.value },
      ],
    }
  })

  // Update translateX when currentIndex changes (for non-gesture updates only)
  useEffect(() => {
    // Only update position if not in the middle of a gesture
    if (!isGestureActive.value) {
      // Since we only render 3 cards max (prev, current, next), current card is always at position 1 (middle)
      // But we need to account for edge cases where there's no previous card
      const positionOffset = currentIndex > 0 ? -cardWidth : 0
      //translateX.value = withSpring(positionOffset, { damping: 20 })
      translateX.value = positionOffset
    }
  }, [currentIndex, cardWidth, translateX])

  const handleCategoryToggle = (category: string) => {
    setSelectedCategories((prev) =>
      prev.includes(category)
        ? prev.filter((c) => c !== category)
        : [...prev, category]
    )
  }

  const handleAllergenToggle = (allergen: string) => {
    setSelectedAllergens((prev) =>
      prev.includes(allergen)
        ? prev.filter((a) => a !== allergen)
        : [...prev, allergen]
    )
  }

  return (
    <View style={$container}>
      {/* Header */}
      <View style={$header}>
        <Text style={$headerTitle}>foodigram</Text>
        
        {/* Search Bar */}
        <View style={$searchContainer}>
          <View style={$searchInputContainer}>
            <TextField
              value={searchQuery}
              onChangeText={setSearchQuery}
              placeholder="Search foods..."
              containerStyle={$searchInput}
              inputWrapperStyle={$searchInputWrapper}
            />
          </View>
          <TouchableOpacity style={$searchButton}>
            <Search size={20} color={colors.palette.neutral100} />
          </TouchableOpacity>
        </View>
      </View>

      {/* Main Content - Takes remaining space */}
      <View style={$mainContent}>
        {filteredFoods.length === 0 ? (
          <View style={$emptyState}>
            <Text style={$emptyText}>No foods found</Text>
            <Text style={$emptySubtext}>Try adjusting your filters or search</Text>
          </View>
        ) : currentFood ? (
          <View style={$carouselContainer}>
            <PanGestureHandler 
              ref={panGestureRef} 
              onGestureEvent={gestureHandler}
              waitFor={[titleScrollGestureRef, allergenScrollGestureRef]}
            >
              <Animated.View style={[$carouselContent, animatedCarouselStyle]}>
                {(() => {
                  // Only render current card and adjacent cards to avoid showing all intermediate cards
                  const cardsToRender = []
                  
                  // Previous card
                  if (currentIndex > 0) {
                    const prevFood = filteredFoods[currentIndex - 1]
                    cardsToRender.push(
                      <View key={prevFood.id} style={[$cardContainer, { width: cardWidth }]}>
                        <View style={$foodCardWrapper}>
                          <FoodCard
                            food={prevFood}
                            isLiked={likedItems.includes(prevFood.id)}
                            isScrapped={foodHistoryStore.isScrapped(prevFood.id)}
                            onLike={() => {
                              setLikedItems((prev) =>
                                prev.includes(prevFood.id)
                                  ? prev.filter((id) => id !== prevFood.id)
                                  : [...prev, prevFood.id]
                              )
                            }}
                            onScrap={() => {
                              foodHistoryStore.toggleScrappedItem(prevFood)
                            }}
                            scale={dynamicStyles.foodCardScale}
                            maxWidth={dynamicStyles.foodCardWidth}
                            titleScrollGestureRef={titleScrollGestureRef}
                            allergenScrollGestureRef={allergenScrollGestureRef}
                          />
                        </View>
                      </View>
                    )
                  }
                  
                  // Current card
                  const currentFood = filteredFoods[currentIndex]
                  if (currentFood) {
                    cardsToRender.push(
                      <View key={currentFood.id} style={[$cardContainer, { width: cardWidth }]}>
                        <View style={$foodCardWrapper}>
                          <FoodCard
                            food={currentFood}
                            isLiked={likedItems.includes(currentFood.id)}
                            isScrapped={foodHistoryStore.isScrapped(currentFood.id)}
                            onLike={() => {
                              setLikedItems((prev) =>
                                prev.includes(currentFood.id)
                                  ? prev.filter((id) => id !== currentFood.id)
                                  : [...prev, currentFood.id]
                              )
                            }}
                            onScrap={() => {
                              foodHistoryStore.toggleScrappedItem(currentFood)
                            }}
                            scale={dynamicStyles.foodCardScale}
                            maxWidth={dynamicStyles.foodCardWidth}
                            titleScrollGestureRef={titleScrollGestureRef}
                            allergenScrollGestureRef={allergenScrollGestureRef}
                          />
                        </View>
                      </View>
                    )
                  }
                  
                  // Next card
                  if (currentIndex < filteredFoods.length - 1) {
                    const nextFood = filteredFoods[currentIndex + 1]
                    cardsToRender.push(
                      <View key={nextFood.id} style={[$cardContainer, { width: cardWidth }]}>
                        <View style={$foodCardWrapper}>
                          <FoodCard
                            food={nextFood}
                            isLiked={likedItems.includes(nextFood.id)}
                            isScrapped={foodHistoryStore.isScrapped(nextFood.id)}
                            onLike={() => {
                              setLikedItems((prev) =>
                                prev.includes(nextFood.id)
                                  ? prev.filter((id) => id !== nextFood.id)
                                  : [...prev, nextFood.id]
                              )
                            }}
                            onScrap={() => {
                              foodHistoryStore.toggleScrappedItem(nextFood)
                            }}
                            scale={dynamicStyles.foodCardScale}
                            maxWidth={dynamicStyles.foodCardWidth}
                            titleScrollGestureRef={titleScrollGestureRef}
                            allergenScrollGestureRef={allergenScrollGestureRef}
                          />
                        </View>
                      </View>
                    )
                  }
                  
                  return cardsToRender
                })()}
              </Animated.View>
            </PanGestureHandler>
          </View>
        ) : null}
      </View>

      {/* Action Buttons */}
      <View style={$actionButtons}>
        <TouchableOpacity
          style={[
            $actionButton, 
            isFilterOpen && $actionButtonHighlighted,
            (isFilterOpen || isFriendsOpen) && !isFilterOpen && $actionButtonDimmed
          ]}
          onPress={() => setIsFilterOpen(true)}
        >
          <Filter size={16} color={colors.palette.neutral700} />
          <Text style={$actionButtonText}>Filter</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[
            $actionButton, 
            showLikedOnly && $actionButtonActive,
            (isFilterOpen || isFriendsOpen) && $actionButtonDimmed
          ]}
          onPress={() => setShowLikedOnly(!showLikedOnly)}
        >
          <Heart 
            size={16} 
            color={colors.palette.neutral700}
            fill={showLikedOnly ? colors.background : "none"}
            stroke={showLikedOnly ? colors.palette.primary500 : colors.palette.neutral700}
          />
          <Text style={[
            $actionButtonText,
            showLikedOnly && { color: colors.background }
          ]}>Liked</Text>
        </TouchableOpacity>

        <TouchableOpacity
          style={[
            $actionButton, 
            isFriendsOpen && $actionButtonHighlighted,
            (isFilterOpen || isFriendsOpen) && !isFriendsOpen && $actionButtonDimmed
          ]}
          onPress={() => setIsFriendsOpen(true)}
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
            // already on Recommendation (Foodigram)
            navigation.navigate("Foodigram")
          }}
        >
          <Home 
            size={28} 
            color={colors.palette.primary500}
          />
        </TouchableOpacity>

        <TouchableOpacity
          style={$tabButton}
          onPress={() => navigation.navigate("Profile")}
        >
          <User 
            size={28} 
            color={colors.palette.neutral500}
          />
        </TouchableOpacity>
      </View>

      {/* Filter Speech Bubble */}
      {isFilterOpen && (
        <TouchableOpacity 
          style={$speechBubbleContainer}
          activeOpacity={1}
          onPress={() => setIsFilterOpen(false)}
        >
          <TouchableOpacity 
            style={$speechBubble}
            activeOpacity={1}
            onPress={(e) => e.stopPropagation()}
          >
            <View style={$speechBubbleHeader}>
              <Text style={$speechBubbleTitle}>Filters</Text>
              <TouchableOpacity onPress={() => setIsFilterOpen(false)}>
                <X size={20} color={colors.palette.neutral700} />
              </TouchableOpacity>
            </View>

            <ScrollView style={$speechBubbleContent} showsVerticalScrollIndicator={false}>
              <Text style={$sectionTitle}>Categories</Text>
              <View style={$filterGrid}>
                {allCategories.map((category) => (
                  <TouchableOpacity
                    key={category}
                    style={[
                      $filterChip,
                      selectedCategories.includes(category) && $filterChipSelected
                    ]}
                    onPress={() => handleCategoryToggle(category)}
                  >
                    <Text 
                      style={[
                        $filterChipText,
                        selectedCategories.includes(category) && $filterChipTextSelected
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
                      selectedAllergens.includes(allergen) && $filterChipSelected
                    ]}
                    onPress={() => handleAllergenToggle(allergen)}
                  >
                    <Text 
                      style={[
                        $filterChipText,
                        selectedAllergens.includes(allergen) && $filterChipTextSelected
                      ]}
                    >
                      {allergen}
                    </Text>
                  </TouchableOpacity>
                ))}
              </View>
            </ScrollView>
          </TouchableOpacity>
        </TouchableOpacity>
      )}

      {/* Friends Speech Bubble */}
      {isFriendsOpen && (
        <TouchableOpacity 
          style={$speechBubbleContainer}
          activeOpacity={1}
          onPress={() => setIsFriendsOpen(false)}
        >
          <TouchableOpacity 
            style={$speechBubble}
            activeOpacity={1}
            onPress={(e) => e.stopPropagation()}
          >
            <View style={$speechBubbleHeader}>
              <Text style={$speechBubbleTitle}>Friends</Text>
              <TouchableOpacity onPress={() => setIsFriendsOpen(false)}>
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
              showsVerticalScrollIndicator={false}
            />
          </TouchableOpacity>
        </TouchableOpacity>
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
  marginTop: spacing.lg,
  marginBottom: spacing.md,
  color: colors.text,
}

const $searchContainer: ViewStyle = {
  flexDirection: "row",
  gap: spacing.sm,
}

const $searchInputContainer: ViewStyle = {
  flex: 1,
}

const $searchInput: ViewStyle = {
  borderRadius: 12,
  backgroundColor: colors.palette.neutral100,
  overflow: "hidden",
}

const $searchInputWrapper: ViewStyle = {
  borderRadius: 12,
  backgroundColor: colors.palette.neutral100,
  overflow: "hidden",
}

const $searchButton: ViewStyle = {
  backgroundColor: colors.palette.primary500,
  borderRadius: 12,
  padding: spacing.sm,
  justifyContent: "center",
  alignItems: "center",
}

const $mainContent: ViewStyle = {
  position: "absolute",
  top: 120, // Header height
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
  backgroundColor: "rgba(0, 0, 0, 0.4)", // Semi-transparent overlay covering full area
  justifyContent: "center",
  alignItems: "center",
  paddingHorizontal: spacing.md,
  zIndex: 2000, // High z-index to appear above all other elements
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

const $carouselContainer: ViewStyle = {
  width: "100%",
  flex: 1,
}

const $carouselContent: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  height: "100%",
}

const $cardContainer: ViewStyle = {
  alignItems: "center",
  justifyContent: "center",
  height: "100%",
}

const $foodCardWrapper: ViewStyle = {
  width: "100%",
  alignItems: "center",
  justifyContent: "center",
}