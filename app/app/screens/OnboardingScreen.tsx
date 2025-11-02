import React, { useState } from "react"
import { observer } from "mobx-react-lite"
import { View, ViewStyle, TextStyle, TouchableOpacity, ScrollView, Alert, Text as RNText } from "react-native"
import { Text } from "app/components"
import { AppStackScreenProps } from "app/navigators"
import { colors, spacing } from "app/theme"
import { api } from "app/services/api"
import { Ionicons } from "@expo/vector-icons"

interface OnboardingScreenProps extends AppStackScreenProps<"Onboarding"> { }

interface UserPreferences {
  spicy_level: number
  sweet_level: number
  salty_level: number
  exploration_preference: number
  allergies: string[]
  disliked_ingredients: string[]
  favorite_cuisines: string[]
}

const ALLERGIES = [
  { id: "eggs", label: "Eggs", icon: "egg-outline" as const },
  { id: "soy", label: "Soy", icon: "leaf-outline" as const },
  { id: "sesame", label: "Sesame", icon: "flower-outline" as const },
  { id: "fish", label: "Fish", icon: "fish-outline" as const },
  { id: "shellfish", label: "Shellfish", icon: "fish-outline" as const },
  { id: "wheat", label: "Wheat", icon: "nutrition-outline" as const },
  { id: "milk", label: "Milk", icon: "water-outline" as const },
  { id: "peanuts", label: "Peanuts", icon: "nutrition-outline" as const },
  { id: "tree nuts", label: "Tree Nuts", icon: "nutrition-outline" as const },
]

const INGREDIENTS = [
  { id: "onion", label: "Onion", icon: "nutrition-outline" as const },
  { id: "garlic", label: "Garlic", icon: "nutrition-outline" as const },
  { id: "ginger", label: "Ginger", icon: "leaf-outline" as const },
  { id: "cilantro", label: "Cilantro", icon: "leaf-outline" as const },
  { id: "mushroom", label: "Mushroom", icon: "nutrition-outline" as const },
  { id: "tomato", label: "Tomato", icon: "nutrition-outline" as const },
  { id: "cheese", label: "Cheese", icon: "pizza-outline" as const },
  { id: "meat", label: "Meat", icon: "restaurant-outline" as const },
  { id: "seafood", label: "Seafood", icon: "fish-outline" as const },
]

const CUISINES = [
  { id: "italian", label: "Italian", icon: "pizza-outline" as const },
  { id: "mexican", label: "Mexican", icon: "fast-food-outline" as const },
  { id: "chinese", label: "Chinese", icon: "restaurant-outline" as const },
  { id: "japanese", label: "Japanese", icon: "fish-outline" as const },
  { id: "indian", label: "Indian", icon: "flame-outline" as const },
  { id: "american", label: "American", icon: "fast-food-outline" as const },
  { id: "thai", label: "Thai", icon: "leaf-outline" as const },
  { id: "mediterranean", label: "Mediterranean", icon: "restaurant-outline" as const },
  { id: "french", label: "French", icon: "wine-outline" as const },
  { id: "vietnamese", label: "Vietnamese", icon: "restaurant-outline" as const },
  { id: "spanish", label: "Spanish", icon: "wine-outline" as const },
  { id: "korean", label: "Korean", icon: "restaurant-outline" as const },
]

export const OnboardingScreen = observer(function OnboardingScreen({ navigation }: OnboardingScreenProps) {
  const [currentStep, setCurrentStep] = useState(0)
  const [preferences, setPreferences] = useState<UserPreferences>({
    spicy_level: 2,
    sweet_level: 2,
    salty_level: 2,
    exploration_preference: 2,
    allergies: [],
    disliked_ingredients: [],
    favorite_cuisines: []
  })
  const [isLoading, setIsLoading] = useState(false)

  const totalSteps = 4

  const handleNext = () => {
    if (currentStep < totalSteps - 1) {
      setCurrentStep(currentStep + 1)
    } else {
      handleComplete()
    }
  }

  const handleSkip = () => {
    navigation.replace("Foodigram")
  }


  const handleComplete = async () => {
    setIsLoading(true)
    try {
      // Convert 0-4 scale to 0-10 scale for API
      const apiPreferences = {
        ...preferences,
        spicy_level: Math.round(preferences.spicy_level * 2.5),
        sweet_level: Math.round(preferences.sweet_level * 2.5),
        salty_level: Math.round(preferences.salty_level * 2.5),
        exploration_preference: Math.round(preferences.exploration_preference * 2.5),
      }
      const response = await api.savePreferences(apiPreferences)
      if (response.ok) {
        Alert.alert("Success", "Your preferences have been saved!", [
          { text: "OK", onPress: () => navigation.replace("Foodigram") }
        ])
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

  const renderProgressBar = () => (
    <View style={$progressContainer}>
      <View style={$progressBar}>
        <View style={[$progressFill, { width: `${((currentStep + 1) / totalSteps) * 100}%` }]} />
      </View>
    </View>
  )

  const renderSlider = (
    label: string,
    value: number,
    minLabel: string,
    maxLabel: string,
    key: 'sweet_level' | 'salty_level' | 'spicy_level' | 'exploration_preference'
  ) => (
    <View style={$sliderContainer}>
      <Text style={$sliderLabel}>{label}</Text>
      <View style={$sliderTrackWrapper}>
        <View style={$sliderTrack}>
          <View style={[$sliderFill, { width: `${(value / 4) * 100}%` }]} />
        </View>
        <View style={$sliderDotsContainer}>
          {[0, 1, 2, 3, 4].map((dotValue) => (
            <TouchableOpacity
              key={dotValue}
              style={$sliderDotTouchable}
              onPress={() => updatePreference(key, dotValue)}
            >
              <View style={[
                $sliderDot,
                dotValue === value && $sliderDotActive
              ]} />
            </TouchableOpacity>
          ))}
        </View>
      </View>
      <View style={$sliderLabels}>
        <Text style={$sliderLabelText}>{minLabel}</Text>
        <Text style={$sliderLabelText}>{maxLabel}</Text>
      </View>
    </View>
  )

  const renderTastePreferences = () => (
    <View style={$stepContainer}>
      <Text style={$stepTitle}>Tell Us Your Taste</Text>
      <Text style={$stepSubtitle}>
        Adjust the sliders to tell us how you like your food. This helps us find the perfect flavor profile for you.
      </Text>
      
      <View style={$slidersWrapper}>
        {renderSlider('Sweet', preferences.sweet_level, 'Not Sweet', 'Very Sweet', 'sweet_level')}
        {renderSlider('Salty', preferences.salty_level, 'Not Salty', 'Very Salty', 'salty_level')}
        {renderSlider('Spicy', preferences.spicy_level, 'Not Spicy', 'Very Spicy', 'spicy_level')}
        {renderSlider(
          'Food Exploration',
          preferences.exploration_preference,
          'Familiar',
          'Adventurous',
          'exploration_preference'
        )}
      </View>
    </View>
  )

  const renderAllergies = () => (
    <View style={$stepContainer}>
      <Text style={$stepTitle}>Any allergies?</Text>
      <Text style={$stepSubtitle}>Select all that apply</Text>
      
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
              <Ionicons 
                name={allergy.icon} 
                size={32} 
                color={preferences.allergies.includes(allergy.id) ? colors.background : colors.text}
              />
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
      <Text style={$stepTitle}>Ingredients you dislike?</Text>
      <Text style={$stepSubtitle}>We'll avoid recommending these</Text>
      
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
              <Ionicons 
                name={ingredient.icon} 
                size={32} 
                color={preferences.disliked_ingredients.includes(ingredient.id) ? colors.background : colors.text}
              />
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
      <Text style={$stepTitle}>What cuisines do you love?</Text>
      <Text style={$stepSubtitle}>Select your favorite cuisines</Text>
      
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
              <Ionicons 
                name={cuisine.icon} 
                size={32} 
                color={preferences.favorite_cuisines.includes(cuisine.id) ? colors.background : colors.text}
              />
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

  const renderCurrentStep = () => {
    switch (currentStep) {
      case 0: return renderTastePreferences()
      case 1: return renderAllergies()
      case 2: return renderDislikedIngredients()
      case 3: return renderFavoriteCuisines()
      default: return renderTastePreferences()
    }
  }

  return (
    <View style={$container}>
      {renderProgressBar()}

      <ScrollView style={$content} showsVerticalScrollIndicator={false}>
        {renderCurrentStep()}
      </ScrollView>

      <View style={$buttonContainer}>
        <TouchableOpacity
          style={$continueButton}
          onPress={handleNext}
          disabled={isLoading}
        >
          <RNText style={$continueButtonText}>
            {currentStep === totalSteps - 1 ? "Complete" : "Continue"}
          </RNText>
        </TouchableOpacity>
        <TouchableOpacity onPress={handleSkip} style={$skipButton}>
          <RNText style={$skipText}>Skip</RNText>
        </TouchableOpacity>
      </View>
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

const $progressText: TextStyle = {
  fontSize: 14,
  fontWeight: "500",
  color: colors.text,
  marginBottom: spacing.xs,
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

const $content: ViewStyle = {
  flex: 1,
  padding: spacing.lg,
}

const $stepContainer: ViewStyle = {
  flex: 1,
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

const $slidersWrapper: ViewStyle = {
  paddingVertical: spacing.lg,
  gap: spacing.xxl,
}

const $syncDescription: TextStyle = {
  fontSize: 14,
  color: colors.palette.neutral500,
  fontStyle: "italic",
  flex: 1,
  marginRight: spacing.sm
}

const $sliderContainer: ViewStyle = {
  gap: spacing.sm,
}

const $sliderLabel: TextStyle = {
  fontSize: 18,
  fontWeight: "bold",
  color: colors.text,
  marginBottom: spacing.xs,
}

const $sliderTrackWrapper: ViewStyle = {
  height: 40,
  justifyContent: "center",
}

const $sliderDescription: TextStyle = {
  fontSize: 14,
  color: colors.palette.neutral600,
  marginBottom: spacing.xs,
}

const $explorationLabels: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  marginBottom: spacing.xs,
}

const $explorationLabelText: TextStyle = {
  fontSize: 12,
  color: colors.palette.neutral500,
  fontStyle: "italic",
}

const $sliderTrack: ViewStyle = {
  position: "absolute",
  width: "100%",
  height: 8,
  backgroundColor: colors.palette.neutral300,
  borderRadius: 4,
}

const $sliderFill: ViewStyle = {
  height: "100%",
  backgroundColor: colors.palette.primary500,
  borderRadius: 4,
}

const $sliderDotsContainer: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
}

const $sliderDotTouchable: ViewStyle = {
  flex: 1,
  alignItems: "center",
  justifyContent: "center",
  paddingVertical: spacing.sm,
}

const $sliderDot: ViewStyle = {
  width: 12,
  height: 12,
  borderRadius: 6,
  backgroundColor: colors.palette.neutral300,
  borderWidth: 2,
  borderColor: colors.background,
}

const $sliderDotActive: ViewStyle = {
  width: 24,
  height: 24,
  borderRadius: 12,
  backgroundColor: colors.palette.primary500,
  borderWidth: 4,
  borderColor: colors.background,
  shadowColor: colors.palette.neutral400,
  shadowOffset: { width: 0, height: 0 },
  shadowOpacity: 0.3,
  shadowRadius: 2,
  elevation: 2,
}

const $sliderLabels: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  paddingHorizontal: spacing.xs,
}

const $sliderLabelText: TextStyle = {
  fontSize: 12,
  color: colors.palette.neutral600,
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
  borderTopWidth: 1,
  borderTopColor: colors.palette.neutral200,
  backgroundColor: colors.background,
  gap: 12,
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
