import React, { useState } from "react"
import { observer } from "mobx-react-lite"
import { View, ViewStyle, TextStyle, TouchableOpacity, ScrollView, Alert } from "react-native"
import { Text } from "app/components"
import { Button } from "app/components/Button"
import { AppStackScreenProps } from "app/navigators"
import { colors, spacing } from "app/theme"
import { api } from "app/services/api"
import { Ionicons } from "@expo/vector-icons"

interface OnboardingScreenProps extends AppStackScreenProps<"Onboarding"> {}

interface UserPreferences {
  spicy_level: number
  sweet_level: number
  salty_level: number
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
    spicy_level: 5,
    sweet_level: 5,
    salty_level: 5,
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
      const response = await api.savePreferences(preferences)
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
      <Text style={$progressText}>Step {currentStep + 1} of {totalSteps}</Text>
      <View style={$progressBar}>
        <View style={[$progressFill, { width: `${((currentStep + 1) / totalSteps) * 100}%` }]} />
      </View>
    </View>
  )

  const renderTastePreferences = () => (
    <View style={$stepContainer}>
      <Text style={$stepTitle}>What are your taste preferences?</Text>
      <Text style={$stepSubtitle}>Help us recommend food you'll love</Text>
      
      <View style={$sliderContainer}>
        <Text style={$sliderLabel}>Spicy Level: {preferences.spicy_level}</Text>
        <View style={$sliderTrack}>
          {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((value) => (
            <TouchableOpacity
              key={value}
              style={[
                $sliderDot,
                value <= preferences.spicy_level ? $sliderDotActive : $sliderDotInactive
              ]}
              onPress={() => updatePreference('spicy_level', value)}
            />
          ))}
        </View>
      </View>

      <View style={$sliderContainer}>
        <Text style={$sliderLabel}>Sweet Level: {preferences.sweet_level}</Text>
        <View style={$sliderTrack}>
          {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((value) => (
            <TouchableOpacity
              key={value}
              style={[
                $sliderDot,
                value <= preferences.sweet_level ? $sliderDotActive : $sliderDotInactive
              ]}
              onPress={() => updatePreference('sweet_level', value)}
            />
          ))}
        </View>
      </View>

      <View style={$sliderContainer}>
        <Text style={$sliderLabel}>Salty Level: {preferences.salty_level}</Text>
        <View style={$sliderTrack}>
          {[0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map((value) => (
            <TouchableOpacity
              key={value}
              style={[
                $sliderDot,
                value <= preferences.salty_level ? $sliderDotActive : $sliderDotInactive
              ]}
              onPress={() => updatePreference('salty_level', value)}
            />
          ))}
        </View>
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
        <Button
          text={currentStep === totalSteps - 1 ? "Complete" : "Continue"}
          style={$continueButton}
          onPress={handleNext}
          disabled={isLoading}
        />
        <TouchableOpacity onPress={handleSkip} style={$skipButton}>
          <Text style={$skipText}>Skip</Text>
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
}

const $stepSubtitle: TextStyle = {
  fontSize: 16,
  color: colors.palette.neutral600,
  marginBottom: spacing.xl,
  textAlign: "left",
}

const $sliderContainer: ViewStyle = {
  marginBottom: spacing.xl,
}

const $sliderLabel: TextStyle = {
  fontSize: 16,
  fontWeight: "600",
  color: colors.text,
  marginBottom: spacing.sm,
}

const $sliderTrack: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
}

const $sliderDot: ViewStyle = {
  width: 20,
  height: 20,
  borderRadius: 10,
  borderWidth: 2,
}

const $sliderDotActive: ViewStyle = {
  backgroundColor: colors.palette.primary500,
  borderColor: colors.palette.primary500,
}

const $sliderDotInactive: ViewStyle = {
  backgroundColor: colors.background,
  borderColor: colors.palette.neutral300,
}

const $cardGrid: ViewStyle = {
  flexDirection: "row",
  flexWrap: "wrap",
  gap: spacing.md,
  justifyContent: "space-between",
}

const $card: ViewStyle = {
  width: "48%",
  backgroundColor: colors.background,
  borderRadius: 16,
  borderWidth: 1,
  borderColor: colors.palette.neutral300,
  padding: spacing.md,
  gap: spacing.sm,
  minHeight: 100,
  justifyContent: "center",
  alignItems: "flex-start",
}

const $cardSelected: ViewStyle = {
  backgroundColor: colors.palette.primary500,
  borderColor: colors.palette.primary500,
}

const $cardText: TextStyle = {
  fontSize: 16,
  fontWeight: "bold",
  color: colors.text,
}

const $cardTextSelected: TextStyle = {
  color: colors.background,
}

const $buttonContainer: ViewStyle = {
  padding: spacing.lg,
  borderTopWidth: 1,
  borderTopColor: colors.palette.neutral200,
  backgroundColor: colors.background,
  gap: spacing.xs,
}

const $continueButton: ViewStyle = {
  width: "100%",
}

const $skipButton: ViewStyle = {
  alignSelf: "center",
  paddingVertical: spacing.sm,
}

const $skipText: TextStyle = {
  fontSize: 16,
  fontWeight: "bold",
  color: colors.text,
}
