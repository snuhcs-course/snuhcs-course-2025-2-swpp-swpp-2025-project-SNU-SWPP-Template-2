import React, { useState } from "react"
import { observer } from "mobx-react-lite"
import { View, ViewStyle, TextStyle, TouchableOpacity, Alert, TextInput, ScrollView } from "react-native"
import { Text } from "app/components"
import { AppStackScreenProps } from "app/navigators"
import * as storage from "app/utils/storage"
import { api } from "app/services/api"
import { Eye, EyeOff } from "lucide-react-native"

interface SignUpScreenProps extends AppStackScreenProps<"SignUp"> {}

export const SignUpScreen = observer(function SignUpScreen({ navigation }: SignUpScreenProps) {
  const [fullName, setFullName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  async function tryRegister() {
    // Form validation
    if (!fullName.trim() || !email.trim() || !password.trim()) {
      Alert.alert("오류", "모든 필드를 입력해주세요.")
      return
    }

    if (password.length < 8) {
      Alert.alert("오류", "비밀번호는 최소 8자 이상이어야 합니다.")
      return
    }

    // Email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email)) {
      Alert.alert("오류", "올바른 이메일 형식을 입력해주세요.")
      return
    }

    setIsLoading(true)
    try {
      // Ensure CSRF cookie is set
      await api.getCsrf()
      const res = await api.register(fullName, email, password)
      if (!res.ok) {
        const errorMessage = (res.data as any)?.detail || "회원가입에 실패했습니다."
        Alert.alert("회원가입 실패", errorMessage)
        return
      }
      
      // Auto login after successful registration
      await storage.saveString("IS_LOGGED_IN", "true")
      Alert.alert("성공", "회원가입이 완료되었습니다!", [
        { 
          text: "확인", 
          onPress: () => {
            // New users always go to onboarding
            navigation.replace("Onboarding")
          }
        }
      ])
    } catch (e) {
      Alert.alert("오류", "회원가입 중 오류가 발생했습니다.")
      console.log(e)
    } finally {
      setIsLoading(false)
    }
  }

  function navigateToLogin() {
    navigation.navigate("Login")
  }

  return (
    <ScrollView 
      style={$container}
      contentContainerStyle={$scrollContent}
      keyboardShouldPersistTaps="handled"
    >
      <View style={$contentWrapper}>
        {/* Title */}
        <Text style={$title}>Create Your Account</Text>
        
        {/* Form */}
        <View style={$form}>
          {/* Full Name */}
          <View style={$inputWrapper}>
            <Text style={$label}>Full Name</Text>
            <TextInput
              style={$input}
              placeholder="Enter your full name"
              placeholderTextColor="#9c5749"
              value={fullName}
              onChangeText={setFullName}
              autoCapitalize="words"
            />
          </View>

          {/* Email Address */}
          <View style={$inputWrapper}>
            <Text style={$label}>Email Address</Text>
            <TextInput
              style={$input}
              placeholder="Enter your email address"
              placeholderTextColor="#9c5749"
              value={email}
              onChangeText={setEmail}
              keyboardType="email-address"
              autoCapitalize="none"
            />
          </View>

          {/* Password */}
          <View style={$inputWrapper}>
            <Text style={$label}>Password</Text>
            <View style={$passwordContainer}>
              <TextInput
                style={$passwordInput}
                placeholder="Enter your password"
                placeholderTextColor="#9c5749"
                secureTextEntry={!showPassword}
                value={password}
                onChangeText={setPassword}
              />
              <TouchableOpacity 
                style={$eyeButton}
                onPress={() => setShowPassword(!showPassword)}
              >
                {showPassword ? (
                  <Eye size={24} color="#9c5749" />
                ) : (
                  <EyeOff size={24} color="#9c5749" />
                )}
              </TouchableOpacity>
            </View>
          </View>

          {/* Sign Up Button */}
          <View style={$buttonWrapper}>
            <TouchableOpacity 
              style={[$signUpButton, isLoading && $signUpButtonDisabled]}
              onPress={tryRegister}
              disabled={isLoading}
            >
              <Text style={$signUpButtonText}>Sign Up</Text>
            </TouchableOpacity>
          </View>

          {/* Login Prompt */}
          <View style={$loginPrompt}>
            <Text style={$loginPromptText}>
              Already have an account?{" "}
              <Text style={$loginLink} onPress={navigateToLogin}>
                Log in
              </Text>
            </Text>
          </View>
        </View>
      </View>
    </ScrollView>
  )
})

const $container: ViewStyle = {
  flex: 1,
  backgroundColor: "#fcf9f8",
}

const $scrollContent: ViewStyle = {
  flexGrow: 1,
  justifyContent: "center",
  paddingHorizontal: 16,
  paddingVertical: 40,
}

const $contentWrapper: ViewStyle = {
  width: "100%",
  maxWidth: 480,
  alignSelf: "center",
  alignItems: "center",
}

const $title: TextStyle = {
  fontSize: 32,
  fontWeight: "700",
  color: "#1c100d",
  textAlign: "center",
  marginBottom: 24,
  paddingTop: 48,
  paddingBottom: 12,
}

const $form: ViewStyle = {
  width: "100%",
}

const $inputWrapper: ViewStyle = {
  width: "100%",
  marginBottom: 12,
  paddingHorizontal: 16,
  paddingVertical: 12,
}

const $label: TextStyle = {
  fontSize: 16,
  fontWeight: "500",
  color: "#1c100d",
  marginBottom: 8,
}

const $input: TextStyle = {
  width: "100%",
  height: 56,
  backgroundColor: "#f4e9e7",
  borderRadius: 12,
  paddingHorizontal: 16,
  fontSize: 16,
  color: "#1c100d",
}

const $passwordContainer: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  backgroundColor: "#f4e9e7",
  borderRadius: 12,
  height: 56,
}

const $passwordInput: TextStyle = {
  flex: 1,
  height: "100%",
  paddingHorizontal: 16,
  fontSize: 16,
  color: "#1c100d",
}

const $eyeButton: ViewStyle = {
  paddingHorizontal: 16,
  justifyContent: "center",
  alignItems: "center",
}

const $buttonWrapper: ViewStyle = {
  width: "100%",
  paddingHorizontal: 16,
  paddingVertical: 24,
}

const $signUpButton: ViewStyle = {
  width: "100%",
  height: 48,
  backgroundColor: "#f66c51",
  borderRadius: 12,
  justifyContent: "center",
  alignItems: "center",
}

const $signUpButtonDisabled: ViewStyle = {
  opacity: 0.6,
}

const $signUpButtonText: TextStyle = {
  fontSize: 16,
  fontWeight: "700",
  color: "#FFFFFF",
}

const $loginPrompt: ViewStyle = {
  marginTop: 16,
  alignItems: "center",
  paddingHorizontal: 16,
  paddingVertical: 8,
}

const $loginPromptText: TextStyle = {
  fontSize: 14,
  color: "#9c5749",
}

const $loginLink: TextStyle = {
  fontSize: 14,
  color: "#f66c51",
  fontWeight: "600",
  textDecorationLine: "underline",
}

