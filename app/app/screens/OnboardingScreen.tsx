import React, { useState, useRef, useEffect } from "react"
import { observer } from "mobx-react-lite"
import { View, ViewStyle, TextStyle, TouchableOpacity, ScrollView, Alert, Text as RNText, Animated, BackHandler } from "react-native"
import { Text, LocationWarningModal, StorageWarningModal, PreferencesCompleteModal } from "app/components"
import { AppStackScreenProps } from "app/navigators"
import { colors, spacing } from "app/theme"
import { api } from "app/services/api"
import { 
  Egg, 
  Leaf, 
  Flower2,
  Fish, 
  UtensilsCrossed,
  Wheat,
  Droplets,
  Circle,
  MapPin,
  Image,
  Camera,
  RotateCcw,
  Images,
  Pizza,
  Coffee,
  ChefHat,
  Flame,
  Wine,
  Sprout,
  Shell,
  Milk,
  Nut
} from "lucide-react-native"
import * as Location from "expo-location"
import * as MediaLibrary from "expo-media-library"
import { useAlbumScanner } from "app/services/albums/useAlbumScanner"
import { match } from "ts-pattern"
import * as storage from "app/utils/storage"

interface OnboardingScreenProps extends AppStackScreenProps<"Onboarding"> { }

interface UserPreferences {
  spicy_level: number | null
  sweet_level: number | null
  salty_level: number | null
  exploration_preference: number | null
  allergies: string[]
  disliked_ingredients: string[]
  favorite_cuisines: string[]
}

const ALLERGIES = [
  { id: "eggs", label: "달걀", icon: "egg" as const },
  { id: "soy", label: "대두", icon: "sprout" as const },
  { id: "sesame", label: "참깨", icon: "flower2" as const },
  { id: "fish", label: "생선", icon: "fish" as const },
  { id: "shellfish", label: "조개", icon: "shell" as const },
  { id: "wheat", label: "밀", icon: "wheat" as const },
  { id: "milk", label: "우유", icon: "milk" as const },
  { id: "peanuts", label: "땅콩", icon: "nut" as const },
  { id: "tree nuts", label: "견과류", icon: "nut" as const },
]

const INGREDIENTS = [
  { id: "onion", label: "양파", icon: "circle" as const },
  { id: "garlic", label: "마늘", icon: "circle" as const },
  { id: "ginger", label: "생강", icon: "leaf" as const },
  { id: "cilantro", label: "고수", icon: "leaf" as const },
  { id: "mushroom", label: "버섯", icon: "circle" as const },
  { id: "tomato", label: "토마토", icon: "circle" as const },
  { id: "cheese", label: "치즈", icon: "pizza" as const },
  { id: "meat", label: "고기", icon: "utensilsCrossed" as const },
  { id: "seafood", label: "해산물", icon: "fish" as const },
]

const CUISINES = [
  { id: "italian", label: "이탈리안", icon: "pizza" as const },
  { id: "mexican", label: "멕시칸", icon: "coffee" as const },
  { id: "chinese", label: "중식", icon: "utensilsCrossed" as const },
  { id: "japanese", label: "일식", icon: "fish" as const },
  { id: "indian", label: "인도", icon: "flame" as const },
  { id: "american", label: "아메리칸", icon: "coffee" as const },
  { id: "thai", label: "태국", icon: "leaf" as const },
  { id: "mediterranean", label: "지중해", icon: "utensilsCrossed" as const },
  { id: "french", label: "프렌치", icon: "wine" as const },
  { id: "vietnamese", label: "베트남", icon: "utensilsCrossed" as const },
  { id: "spanish", label: "스페인", icon: "wine" as const },
  { id: "korean", label: "한식", icon: "chefHat" as const },
]

// 새로운 입맛 스텝 정의
type TasteStep =
  | "location"
  | "gallery"
  | "sweet"
  | "spicy"
  | "salty"
  | "exploration"
  | "allergies"
  | "disliked"
  | "cuisine"

const TASTE_STEPS: TasteStep[] = [
  "location",
  "gallery",
  "sweet",
  "spicy",
  "salty",
  "exploration",
  "allergies",
  "disliked",
  "cuisine",
]

// Icon rendering function
const renderIcon = (iconName: string, size: number, color: string) => {
  switch (iconName) {
    case "egg": return <Egg size={size} color={color} />
    case "sprout": return <Sprout size={size} color={color} />
    case "leaf": return <Leaf size={size} color={color} />
    case "flower2": return <Flower2 size={size} color={color} />
    case "fish": return <Fish size={size} color={color} />
    case "shell": return <Shell size={size} color={color} />
    case "utensilsCrossed": return <UtensilsCrossed size={size} color={color} />
    case "wheat": return <Wheat size={size} color={color} />
    case "milk": return <Milk size={size} color={color} />
    case "nut": return <Nut size={size} color={color} />
    case "droplets": return <Droplets size={size} color={color} />
    case "circle": return <Circle size={size} color={color} />
    case "pizza": return <Pizza size={size} color={color} />
    case "coffee": return <Coffee size={size} color={color} />
    case "flame": return <Flame size={size} color={color} />
    case "wine": return <Wine size={size} color={color} />
    case "chefHat": return <ChefHat size={size} color={color} />
    default: return <Circle size={size} color={color} />
  }
}

export const OnboardingScreen = observer(function OnboardingScreen({ navigation }: OnboardingScreenProps) {
  const [currentStepIdx, setCurrentStepIdx] = useState(0)
  const [preferences, setPreferences] = useState<UserPreferences>({
    spicy_level: null,
    sweet_level: null,
    salty_level: null,
    exploration_preference: null,
    allergies: [],
    disliked_ingredients: [],
    favorite_cuisines: []
  })
  const [isLoading, setIsLoading] = useState(false)
  const [locationPermissionGranted, setLocationPermissionGranted] = useState(false)
  const [galleryPermissionGranted, setGalleryPermissionGranted] = useState(false)
  const [locationWarningVisible, setLocationWarningVisible] = useState(false)
  const [storageWarningVisible, setStorageWarningVisible] = useState(false)
  const [useDummyLocation, setUseDummyLocation] = useState(false)
  const [useDummyStorage, setUseDummyStorage] = useState(false)
  const [preferencesCompleteVisible, setPreferencesCompleteVisible] = useState(false)
  const { scanAlbums } = useAlbumScanner()
  const [galleryPermissionResponse, requestGalleryPermission] = MediaLibrary.usePermissions(
    { granularPermissions: ['photo'] }
  )

  const currentStep = TASTE_STEPS[currentStepIdx]
  const totalSteps = TASTE_STEPS.length

  // 프로그레스바 애니메이션
  const progressAnim = useRef(new Animated.Value(0)).current

  useEffect(() => {
    const targetProgress = ((currentStepIdx + 1) / totalSteps) * 100
    Animated.timing(progressAnim, {
      toValue: targetProgress,
      duration: 300,
      useNativeDriver: false,
    }).start()
  }, [currentStepIdx, totalSteps])

  // Sync gallery permission status with hook response
  useEffect(() => {
    if (galleryPermissionResponse?.status === 'granted') {
      setGalleryPermissionGranted(true)
    }
  }, [galleryPermissionResponse?.status])

  const handleLocationPermission = async () => {
    const moveToNextStep = () => {
      if (currentStepIdx < totalSteps - 1) {
        setCurrentStepIdx(currentStepIdx + 1)
      }
    }

    const { status } = await Location.requestForegroundPermissionsAsync()
    if (status === 'granted') {
      setLocationPermissionGranted(true)
      // Try to get current location
      try {
        await Location.getCurrentPositionAsync({})
      } catch (error) {
        console.log("Location error:", error)
      }
      moveToNextStep()
    }
  }

  const handleGalleryPermission = async () => {
    const moveToNextStep = () => {
      if (currentStepIdx < totalSteps - 1) {
        setCurrentStepIdx(currentStepIdx + 1)
      }
    }

    try {
      // Request only photo permissions, not audio/music permissions (using usePermissions hook)
      let permissionStatus = galleryPermissionResponse?.status
      if (permissionStatus !== 'granted') {
        const response = await requestGalleryPermission()
        permissionStatus = response?.status
      }
      
      if (permissionStatus === 'granted') {
        setGalleryPermissionGranted(true)
        // Start scanning albums in the background
        scanAlbums(() => {
          console.log("Gallery image synced")
        })
        moveToNextStep()
      } else {
        // Permission denied - show warning modal
        setStorageWarningVisible(true)
      }
    } catch (error) {
      console.log("Gallery permission error:", error)
      // Show warning on error
      setStorageWarningVisible(true)
    }
  }

  const handleNext = () => {
    if (currentStepIdx < totalSteps - 1) {
      setCurrentStepIdx(currentStepIdx + 1)
    } else {
      handleComplete()
    }
  }

  const handleBack = () => {
    if (currentStepIdx > 0) {
      setCurrentStepIdx(currentStepIdx - 1)
    }
  }

  // Prevent hardware back button from going back to initial screen
  useEffect(() => {
    const onBackPress = () => {
      // If on first step (location), don't allow going back to Welcome/Login/SignUp
      if (currentStepIdx === 0) {
        return true // Handled - prevent default back behavior
      }
      // For other steps, use the built-in handleBack function
      handleBack()
      return true // Handled
    }

    const subscription = BackHandler.addEventListener('hardwareBackPress', onBackPress)
    return () => subscription.remove()
  }, [currentStepIdx])

  const handleSkip = () => {
    if (currentStep === "location") {
      setLocationWarningVisible(true)
    } else if (currentStep === "gallery") {
      setStorageWarningVisible(true)
    } else {
      // For other steps, just skip to next
      handleNext()
    }
  }

  const handleLocationWarningConfirm = () => {
    setLocationWarningVisible(false)
    setUseDummyLocation(true)
    // Continue to next step with dummy location
    if (currentStepIdx < totalSteps - 1) {
      setCurrentStepIdx(currentStepIdx + 1)
    }
  }

  const handleLocationWarningCancel = () => {
    setLocationWarningVisible(false)
    // Stay on location step to allow GPS usage
  }

  const handleStorageWarningConfirm = () => {
    setStorageWarningVisible(false)
    setUseDummyStorage(true)
    // Continue to next step with dummy storage
    if (currentStepIdx < totalSteps - 1) {
      setCurrentStepIdx(currentStepIdx + 1)
    }
  }

  const handleStorageWarningCancel = () => {
    setStorageWarningVisible(false)
    // Stay on storage step to allow gallery access
  }

  const handlePreferencesComplete = async () => {
    setPreferencesCompleteVisible(false)
    // Clear NEW_USER flag since onboarding is complete
    await storage.remove("NEW_USER")
    
    // Save permission states for settings sync
    const finalGalleryGranted = galleryPermissionGranted || galleryPermissionResponse?.status === 'granted'
    
    await storage.saveString("LOCATION_PERMISSION_GRANTED", locationPermissionGranted.toString())
    await storage.saveString("GALLERY_PERMISSION_GRANTED", finalGalleryGranted.toString())
    await storage.saveString("USE_DUMMY_LOCATION", useDummyLocation.toString())
    await storage.saveString("USE_DUMMY_STORAGE", useDummyStorage.toString())
    
    if (__DEV__) {
      console.log("Onboarding - Saving permission states:", {
        locationPermissionGranted,
        galleryPermissionGranted: finalGalleryGranted,
        useDummyLocation,
        useDummyStorage,
      })
    }
    
    navigation.replace("Foodigram")
  }

  const handleComplete = async () => {
    setIsLoading(true)

    if (preferences.spicy_level === null || preferences.sweet_level === null || preferences.salty_level === null || preferences.exploration_preference === null) {
      // TODO: assert error
      setIsLoading(false)
      return
    }

    try {
      // Convert to 0-10 scale for API as appropriate
      const apiPreferences = {
        ...preferences,
        spicy_level: Math.round(preferences.spicy_level * 2.5),
        sweet_level: Math.round(preferences.sweet_level * 2.5),
        salty_level: Math.round(preferences.salty_level * 5), // if we map 0~2 to 0/5/10
        exploration_preference: preferences.exploration_preference === 0 ? 5 : 0
      }
      const response = await api.savePreferences(apiPreferences)
      if (response.ok) {
        setPreferencesCompleteVisible(true)
      } else {
        Alert.alert("Error", "Failed to save preferences. Please try again.")
      }
    } catch (error) {
      Alert.alert("Error", "An error occurred while saving preferences.")
      console.error("Onboarding error:", error)
    } finally {
      setIsLoading(false)
    }
  }

  const updatePreference = (key: keyof UserPreferences, value: any) => {
    setPreferences(prev => ({ ...prev, [key]: value }))
  }

  const toggleArrayItem = (key: 'allergies' | 'disliked_ingredients' | 'favorite_cuisines', item: string) => {
    setPreferences(prev => ({
      ...prev,
      [key]: prev[key].includes(item)
        ? prev[key].filter(i => i !== item)
        : [...prev[key], item]
    }))
  }

  // Taste choice button rendering
  interface TasteOption {
    label: string
    value: number
  }

  // Q/A 옵션 세트
  const sweetOptions: TasteOption[] = [
    { label: "매우 좋아해요", value: 4 },
    { label: "좋아해요", value: 3 },
    { label: "평범해요", value: 2 },
    { label: "싫어해요", value: 1 },
    { label: "절대 안 먹어요", value: 0 },
  ]
  const spicyOptions: TasteOption[] = [
    { label: "매우 좋아해요", value: 4 },
    { label: "좋아해요", value: 3 },
    { label: "평범해요", value: 2 },
    { label: "싫어해요", value: 1 },
    { label: "절대 안 먹어요", value: 0 },
  ]
  const saltyOptions: TasteOption[] = [
    { label: "짜게 먹어요", value: 2 },
    { label: "평범해요", value: 1 },
    { label: "싱겁게 먹어요", value: 0 },
  ]
  const explorationOptions: TasteOption[] = [
    { label: "좋아해요", value: 0 },           // 0: adventurous, will map to 10 for API
    { label: "먹던 거만 먹어요", value: 1 },   // 1: not adventurous, will map to 0 for API
  ]

  const renderTasteStep = (
    question: string,
    key: keyof UserPreferences,
    options: TasteOption[],
    value: number | null
  ) => (
    <View style={$stepContainer}>
      <Text style={$tasteStepTitle}>{question}</Text>
      <View style={$tasteOptionsContainer}>
        {options.map(opt => (
          <TouchableOpacity
            key={opt.value}
            style={[
              $tasteOptionButton,
              value === opt.value && $tasteOptionButtonActive,
            ]}
            onPress={() => updatePreference(key, opt.value)}
            activeOpacity={0.8}
          >
            <Text
              style={[
                $tasteOptionText,
                value === opt.value && $tasteOptionTextActive,
              ]}
            >
              {opt.label}
            </Text>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  )

  // 기존 onboarding 스텝들 유지하되 taste 단계는 각각 별도 화면으로 대체
  const renderAllergies = () => (
    <View style={$stepContainer}>
      <Text style={$stepTitle}>알러지가 있나요?</Text>
      <Text style={$stepSubtitle}>선택해주신 재료들은 추천에서 제외돼요</Text>
      <View style={$cardGrid}>
        {ALLERGIES.map((allergy) => (
          <TouchableOpacity
            key={allergy.id}
            style={[
              $card,
              preferences.allergies.includes(allergy.id) && $cardSelected
            ]}
            onPress={() => toggleArrayItem('allergies', allergy.id)}
            activeOpacity={0.7}
          >
            <View style={$cardContent}>
              {renderIcon(
                allergy.icon,
                32,
                preferences.allergies.includes(allergy.id) ? colors.background : colors.text
              )}
              <Text style={[
                $cardText,
                preferences.allergies.includes(allergy.id) && $cardTextSelected
              ]}>
                {allergy.label}
              </Text>
            </View>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  )

  const renderDislikedIngredients = () => (
    <View style={$stepContainer}>
      <Text style={$stepTitle}>싫어하는 재료가 있나요?</Text>
      <Text style={$stepSubtitle}>선택해주신 재료들은 추천에서 제외돼요</Text>
      <View style={$cardGrid}>
        {INGREDIENTS.map((ingredient) => (
          <TouchableOpacity
            key={ingredient.id}
            style={[
              $card,
              preferences.disliked_ingredients.includes(ingredient.id) && $cardSelected
            ]}
            onPress={() => toggleArrayItem('disliked_ingredients', ingredient.id)}
            activeOpacity={0.7}
          >
            <View style={$cardContent}>
              {renderIcon(
                ingredient.icon,
                32,
                preferences.disliked_ingredients.includes(ingredient.id) ? colors.background : colors.text
              )}
              <Text style={[
                $cardText,
                preferences.disliked_ingredients.includes(ingredient.id) && $cardTextSelected
              ]}>
                {ingredient.label}
              </Text>
            </View>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  )

  const renderFavoriteCuisines = () => (
    <View style={$stepContainer}>
      <Text style={$stepTitle}>좋아하는 음식 종류가 있나요?</Text>
      <Text style={$stepSubtitle}>선택해주신 음식 종류들을 위주로 추천해드려요</Text>
      <View style={$cardGrid}>
        {CUISINES.map((cuisine) => (
          <TouchableOpacity
            key={cuisine.id}
            style={[
              $card,
              preferences.favorite_cuisines.includes(cuisine.id) && $cardSelected
            ]}
            onPress={() => toggleArrayItem('favorite_cuisines', cuisine.id)}
            activeOpacity={0.7}
          >
            <View style={$cardContent}>
              {renderIcon(
                cuisine.icon,
                32,
                preferences.favorite_cuisines.includes(cuisine.id) ? colors.background : colors.text
              )}
              <Text style={[
                $cardText,
                preferences.favorite_cuisines.includes(cuisine.id) && $cardTextSelected
              ]}>
                {cuisine.label}
              </Text>
            </View>
          </TouchableOpacity>
        ))}
      </View>
    </View>
  )

  const renderLocationPermission = () => (
    <View style={$stepContainer}>
      <View style={$locationIconContainer}>
        <MapPin
          size={80} 
          color={locationPermissionGranted ? colors.palette.primary500 : colors.palette.neutral400}
        />
      </View>
      <Text style={$stepTitle}>위치 서비스 활성화</Text>
      <Text style={$stepSubtitle}>
        {'근처의 맛집을 추천하기 위해 위치 정보가 필요해요.'}
      </Text>
      <View style={$locationBenefitsContainer}>
        <View style={$benefitItem}>
          <UtensilsCrossed size={24} color={colors.palette.primary500} />
          <Text style={$benefitText}>주변 레스토랑</Text>
        </View>
        <View style={$benefitItem}>
          <MapPin size={24} color={colors.palette.primary500} />
          <Text style={$benefitText}>맞춤형 추천</Text>
        </View>
        <View style={$benefitItem}>
          <MapPin size={24} color={colors.palette.primary500} />
          <Text style={$benefitText}>새로운 맛집 경험</Text>
        </View>
      </View>
    </View>
  )

  const renderGalleryPermission = () => {
    const isGranted = galleryPermissionGranted || galleryPermissionResponse?.status === 'granted'
    return (
    <View style={$stepContainer}>
      <View style={$locationIconContainer}>
        <Images
          size={80} 
          color={isGranted ? colors.palette.primary500 : colors.palette.neutral400}
        />
      </View>
      <Text style={$stepTitle}>갤러리 동기화</Text>
      <Text style={$stepSubtitle}>
        {'갤러리의 음식 사진을 자동으로 동기화해서 추천에 활용해요.'}
      </Text>
      <View style={$locationBenefitsContainer}>
        <View style={$benefitItem}>
          <Camera size={24} color={colors.palette.primary500} />
          <Text style={$benefitText}>음식 사진 자동 인식</Text>
        </View>
        <View style={$benefitItem}>
          <RotateCcw size={24} color={colors.palette.primary500} />
          <Text style={$benefitText}>맞춤형 추천 향상</Text>
        </View>
        <View style={$benefitItem}>
          <Image size={24} color={colors.palette.primary500} />
          <Text style={$benefitText}>개인 갤러리 관리</Text>
        </View>
      </View>
    </View>
    )
  }

  const renderCurrentStep = () => {
    switch (currentStep) {
      case "location":
        return renderLocationPermission()
      case "gallery":
        return renderGalleryPermission()
      case "sweet":
        return renderTasteStep("단 걸 좋아하시나요?", "sweet_level", sweetOptions, preferences.sweet_level)
      case "spicy":
        return renderTasteStep("매운 걸 좋아하시나요?", "spicy_level", spicyOptions, preferences.spicy_level)
      case "salty":
        return renderTasteStep("짜게 드시는 편인가요?", "salty_level", saltyOptions, preferences.salty_level)
      case "exploration":
        return renderTasteStep("새로운 음식을 좋아하시나요?", "exploration_preference", explorationOptions, preferences.exploration_preference)
      case "allergies":
        return renderAllergies()
      case "disliked":
        return renderDislikedIngredients()
      case "cuisine":
        return renderFavoriteCuisines()
      default:
        return null
    }
  }

  const progressWidth = progressAnim.interpolate({
    inputRange: [0, 100],
    outputRange: ['0%', '100%'],
  })

  return (
    <View style={$container}>
      <View style={$progressContainer}>
        <View style={$progressBar}>
          <Animated.View style={[$progressFill, { width: progressWidth }]} />
        </View>
      </View>
      <ScrollView contentContainerStyle={$contentContainer} style={$content} showsVerticalScrollIndicator={false}>
        {renderCurrentStep()}
      </ScrollView>
      <View style={$buttonContainer}>
        {match(currentStep).with("location", () => (
          <>
          <TouchableOpacity style={$continueButton} onPress={handleLocationPermission} disabled={isLoading}>
            <RNText style={$continueButtonText}>위치 접근 허용</RNText>
          </TouchableOpacity>
          <TouchableOpacity onPress={handleSkip} style={$skipButton}>
          <RNText style={$skipText}>건너뛰기</RNText>
        </TouchableOpacity>
        </>
        )).with("gallery", () => (
          <>
          <TouchableOpacity style={$continueButton} onPress={handleGalleryPermission} disabled={isLoading}>
            <RNText style={$continueButtonText}>갤러리 접근 허용</RNText>
          </TouchableOpacity>
          <TouchableOpacity onPress={handleSkip} style={$skipButton}>
          <RNText style={$skipText}>건너뛰기</RNText>
        </TouchableOpacity>
        </>
        )).with("allergies", 'disliked', () => (
          <>
            <TouchableOpacity style={$continueButton} onPress={handleNext} disabled={isLoading}>
              <RNText style={$continueButtonText}>다음</RNText>
            </TouchableOpacity>
            <TouchableOpacity style={$skipButton} onPress={handleBack} disabled={isLoading}>
              <RNText style={$skipText}>뒤로</RNText>
            </TouchableOpacity>
          </>
        )).with('sweet', 'spicy', 'salty', 'exploration', () => {
          const isDisabled = (() => {
            switch (currentStep) {
              case 'sweet':
                return preferences.sweet_level === null
              case 'spicy':
                return preferences.spicy_level === null
              case 'salty':
                return preferences.salty_level === null
              case 'exploration':
                return preferences.exploration_preference === null
              default:
                return false
            }
          })()
          
          return (
            <>  
              <TouchableOpacity 
                style={[
                  $continueButton,
                  (isDisabled || isLoading) && $continueButtonDisabled
                ]} 
                onPress={handleNext} 
                disabled={isDisabled || isLoading}
              >
                <RNText style={[
                  $continueButtonText,
                  (isDisabled || isLoading) && $continueButtonTextDisabled
                ]}>
                  다음
                </RNText>
              </TouchableOpacity>
              <TouchableOpacity style={$skipButton} onPress={handleBack} disabled={isLoading}>
                <RNText style={$skipText}>뒤로</RNText>
              </TouchableOpacity>
            </>
          )
        }).with("cuisine", () => (
          <>
            <TouchableOpacity style={$continueButton} onPress={handleComplete} disabled={isLoading}>
              <RNText style={$continueButtonText}>완료</RNText>
            </TouchableOpacity>
            <TouchableOpacity style={$skipButton} onPress={handleBack} disabled={isLoading}>
              <RNText style={$skipText}>뒤로</RNText>
            </TouchableOpacity>
          </>
        )).otherwise(() => (
          <></>
        ))}
      </View>

      <LocationWarningModal
        visible={locationWarningVisible}
        onCancel={handleLocationWarningCancel}
        onConfirm={handleLocationWarningConfirm}
      />

      <StorageWarningModal
        visible={storageWarningVisible}
        onCancel={handleStorageWarningCancel}
        onConfirm={handleStorageWarningConfirm}
      />

      <PreferencesCompleteModal
        visible={preferencesCompleteVisible}
        onConfirm={handlePreferencesComplete}
      />
    </View>
  )
})

const $container: ViewStyle = {
  flex: 1,
  backgroundColor: colors.background,
}

const $progressContainer: ViewStyle = {
  padding: spacing.lg,
  paddingTop: spacing.xl,
  marginTop: spacing.xl,
  gap: spacing.xs,
}

const $progressBar: ViewStyle = {
  height: 8,
  backgroundColor: colors.palette.neutral200,
  borderRadius: 9999,
  overflow: "hidden",
}

const $progressFill: ViewStyle = {
  height: "100%",
  backgroundColor: colors.palette.primary500,
  borderRadius: 9999,
}

const $contentContainer: ViewStyle = {
  paddingBottom: spacing.lg,
}

const $content: ViewStyle = {
  flex: 1,
  padding: spacing.lg,
}

const $stepContainer: ViewStyle = {
  flex: 1,
  minHeight: 300,
}

const $tasteStepTitle: TextStyle = {
  fontSize: 28,
  fontWeight: "bold",
  color: colors.text,
  marginBottom: spacing.xl,
  marginTop: spacing.xl,
  textAlign: "left",
  lineHeight: 38,
}

const $tasteOptionsContainer: ViewStyle = {
  flexDirection: "column",
  gap: spacing.xs,
}

const $tasteOptionButton: ViewStyle = {
  paddingVertical: spacing.lg,
  paddingHorizontal: spacing.md,
  backgroundColor: colors.palette.neutral100,
  borderRadius: 12,
  borderWidth: 1,
  borderColor: colors.palette.neutral200,
}

const $tasteOptionButtonActive: ViewStyle = {
  backgroundColor: colors.palette.primary500,
  borderColor: colors.palette.primary500,
}

const $tasteOptionText: TextStyle = {
  fontSize: 18,
  color: colors.text,
  fontWeight: "600",
  textAlign: "center"
}

const $tasteOptionTextActive: TextStyle = {
  color: "#fff",
}

const $stepTitle: TextStyle = {
  fontSize: 32,
  fontWeight: "bold",
  color: colors.text,
  marginBottom: spacing.sm,
  textAlign: "left",
  lineHeight: 40,
}

const $stepSubtitle: TextStyle = {
  fontSize: 16,
  color: colors.palette.neutral600,
  marginBottom: spacing.xl,
  textAlign: "left",
  lineHeight: 24,
}

const $cardGrid: ViewStyle = {
  flexDirection: "row",
  flexWrap: "wrap",
  justifyContent: "space-between",
}

const $card: ViewStyle = {
  width: "48%",
  backgroundColor: colors.background,
  borderRadius: 16,
  borderWidth: 1,
  borderColor: colors.palette.neutral300,
  padding: spacing.md,
  minHeight: 100,
  marginBottom: spacing.md,
  justifyContent: "flex-start",
  alignItems: "flex-start",
}

const $cardSelected: ViewStyle = {
  backgroundColor: colors.palette.primary500,
  borderColor: colors.palette.primary500,
}

const $cardContent: ViewStyle = {
  flex: 1,
  gap: spacing.sm,
}

const $cardText: TextStyle = {
  fontSize: 16,
  fontWeight: "bold",
  color: colors.text,
  flexShrink: 1,
}

const $cardTextSelected: TextStyle = {
  color: colors.background,
}

const $buttonContainer: ViewStyle = {
  padding: spacing.lg,
  paddingTop: spacing.sm,
  borderTopWidth: 1,
  borderTopColor: colors.palette.neutral200,
  backgroundColor: colors.background,
  gap: 8,
}

const $continueButton: ViewStyle = {
  backgroundColor: "#f66c51",
  borderRadius: 12,
  height: 48,
  paddingHorizontal: 20,
  alignItems: "center",
  justifyContent: "center",
  width: "100%",
}

const $continueButtonText: TextStyle = {
  color: "#FFFFFF",
  fontSize: 16,
  fontWeight: "bold",
}

const $continueButtonDisabled: ViewStyle = {
  backgroundColor: colors.palette.neutral300,
  opacity: 0.6,
}

const $continueButtonTextDisabled: TextStyle = {
  color: colors.palette.neutral600,
}

const $skipButton: ViewStyle = {
  backgroundColor: "rgba(255, 255, 255, 0.2)",
  borderRadius: 12,
  height: 48,
  paddingHorizontal: 20,
  alignItems: "center",
  justifyContent: "center",
  width: "100%",
}

const $skipText: TextStyle = {
  color: colors.text,
  fontSize: 16,
  fontWeight: "bold",
}

const $locationIconContainer: ViewStyle = {
  alignItems: "center",
  justifyContent: "center",
  marginBottom: spacing.xl,
  marginTop: spacing.xl,
}

const $locationBenefitsContainer: ViewStyle = {
  gap: spacing.md,
  marginTop: spacing.xl,
}

const $benefitItem: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  gap: spacing.md,
  paddingVertical: spacing.sm,
}

const $benefitText: TextStyle = {
  fontSize: 16,
  color: colors.text,
  flex: 1,
}
