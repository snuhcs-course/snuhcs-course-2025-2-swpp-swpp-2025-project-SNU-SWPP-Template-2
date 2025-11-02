import { Text } from "app/components"
import { AppStackScreenProps } from "app/navigators"
import { api } from "app/services/api"
import { handleSignIn } from "app/services/aws/handleAwsSignin"
import * as storage from "app/utils/storage"
import { Eye, EyeOff } from "lucide-react-native"
import { observer } from "mobx-react-lite"
import React, { useState } from "react"
import { Alert, TextInput, TextStyle, TouchableOpacity, View, ViewStyle } from "react-native"

interface LoginScreenProps extends AppStackScreenProps<"Login"> { }

export const LoginScreen = observer(function LoginScreen({ navigation }: LoginScreenProps) {
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  async function tryLogin() {
    if (!username.trim() || !password.trim()) {
      Alert.alert("Error", "Please enter username and password.")
      return
    }

    setIsLoading(true)
    try {
      // Ensure CSRF cookie is set
      await api.getCsrf()
      const res = await api.login(username, password)
      if (!res.ok) {
        const errorMessage = (res.data as any)?.detail || "Login failed."
        Alert.alert("Login Failed", errorMessage)
        return
      }

      // log into aws amplify
      await handleSignIn(username, password);

      await storage.saveString("IS_LOGGED_IN", "true")

      // Check if user has preferences set
      try {
        const preferencesResponse = await api.getPreferences()
        if (preferencesResponse.ok && preferencesResponse.data) {
          // User has preferences, go directly to main app
          navigation.replace("Foodigram")
        } else {
          // User has no preferences, show onboarding
          navigation.replace("Onboarding")
        }
      } catch (error) {
        // If error checking preferences, show onboarding to be safe
        navigation.replace("Onboarding")
      }
    } catch (e) {
      Alert.alert("Error", "An error occurred during login.")
      console.log(e)
    } finally {
      setIsLoading(false)
    }
  }

  function navigateToSignUp() {
    navigation.navigate("SignUp")
  }

  return (
    <View style={$containerNew}>
      <View style={$contentWrapper}>
        <View style={$formNew}>
          <View style={$inputWrapper}>
            <Text style={$label}>Username</Text>
            <TextInput
              style={$input}
              placeholder="Enter your username"
              placeholderTextColor="#9c5749"
              value={username}
              onChangeText={setUsername}
              autoCapitalize="none"
            />
          </View>

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

          <TouchableOpacity style={$forgotPassword}>
            <Text style={$forgotPasswordText}>Forgot Password?</Text>
          </TouchableOpacity>

          <TouchableOpacity 
            style={[$loginButton, isLoading && $loginButtonDisabled]}
            onPress={tryLogin}
            disabled={isLoading}
          >
            <Text style={$loginButtonText}>Log In</Text>
          </TouchableOpacity>

          <View style={$signUpPrompt}>
            <Text style={$signUpPromptText}>
              Don't have an account?{" "}
              <Text style={$signUpLink} onPress={navigateToSignUp}>
                Sign Up
              </Text>
            </Text>
          </View>
        </View>
      </View>
    </View>
  )
})

const $containerNew: ViewStyle = {
  flex: 1,
  backgroundColor: "#f8f6f5",
  justifyContent: "center",
  alignItems: "center",
  paddingHorizontal: 16,
  paddingVertical: 40,
}

const $contentWrapper: ViewStyle = {
  width: "100%",
  maxWidth: 480,
  alignItems: "center",
}

const $formNew: ViewStyle = {
  width: "100%",
}

const $inputWrapper: ViewStyle = {
  width: "100%",
  marginBottom: 12,
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

const $forgotPassword: ViewStyle = {
  alignSelf: "flex-end",
  marginTop: 4,
  marginBottom: 12,
}

const $forgotPasswordText: TextStyle = {
  fontSize: 14,
  color: "#f66c51",
  textDecorationLine: "underline",
}

const $loginButton: ViewStyle = {
  width: "100%",
  height: 48,
  backgroundColor: "#f66c51",
  borderRadius: 12,
  justifyContent: "center",
  alignItems: "center",
  marginTop: 12,
}

const $loginButtonDisabled: ViewStyle = {
  opacity: 0.6,
}

const $loginButtonText: TextStyle = {
  fontSize: 16,
  fontWeight: "700",
  color: "#FFFFFF",
}

const $signUpPrompt: ViewStyle = {
  marginTop: 16,
  alignItems: "center",
}

const $signUpPromptText: TextStyle = {
  fontSize: 14,
  color: "#9c5749",
  textAlign: "center",
}

const $signUpLink: TextStyle = {
  fontSize: 14,
  color: "#f66c51",
  fontWeight: "500",
  textDecorationLine: "underline",
}
