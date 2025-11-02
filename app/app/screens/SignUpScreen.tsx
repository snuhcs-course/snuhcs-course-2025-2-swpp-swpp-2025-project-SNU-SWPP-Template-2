import React, { useState } from "react"
import { observer } from "mobx-react-lite"
import { View, ViewStyle, TextStyle, TouchableOpacity, Alert, TextInput, ScrollView } from "react-native"
import { Text } from "app/components"
import { AppStackScreenProps } from "app/navigators"
import * as storage from "app/utils/storage"
import { api } from "app/services/api"
import { Eye, EyeOff } from "lucide-react-native"
import { signUp } from "aws-amplify/auth"

interface SignUpScreenProps extends AppStackScreenProps<"SignUp"> {}

export const SignUpScreen = observer(function SignUpScreen({ navigation }: SignUpScreenProps) {
  const [fullName, setFullName] = useState("")
  const [email, setEmail] = useState("")
  const [password, setPassword] = useState("")
  const [isLoading, setIsLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  type SignUpParameters = {
    username: string;
    password: string;
    email: string;
  };

  async function handleAwsSignUp({
    username,
    password,
    email,
  }: SignUpParameters) {
    try {
      const { isSignUpComplete, userId, nextStep } = await signUp({
        username,
        password,
        options: {
          userAttributes: {
            email,
          },
          // optional
          autoSignIn: true, // or SignInOptions e.g { authFlowType: "USER_SRP_AUTH" }
        },
      });

      console.log(`AWS Amplify userId: ${userId}`);

      if (!isSignUpComplete) {
        throw new Error(`Sign up not complete - something wrong with autosignin. Next step: ${JSON.stringify(nextStep)}`);
      }
    } catch (error) {
      console.log('error signing up:', error);
    }
  }

  async function tryRegister() {
    // Form validation
    if (!fullName.trim() || !email.trim() || !password.trim()) {
      Alert.alert("Error", "Please fill in all fields.")
      return
    }

    if (password.length < 8) {
      Alert.alert("Error", "Password must be at least 8 characters long.")
      return
    }

    // Email format validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(email)) {
      Alert.alert("Error", "Please enter a valid email address.")
      return
    }

    setIsLoading(true)
    try {
      // Ensure CSRF cookie is set
      await api.getCsrf()
      const res = await api.register(fullName, email, password)
      handleAwsSignUp({username: fullName, email, password});
      if (!res.ok) {
        const errorMessage = (res.data as any)?.detail || "Sign up failed."
        Alert.alert("Sign Up Failed", errorMessage)
        return
      }
      
      // Auto login after successful registration
      await storage.saveString("IS_LOGGED_IN", "true")
      Alert.alert("Success", "Your account has been created!", [
        { 
          text: "OK", 
          onPress: () => {
            // New users always go to onboarding
            navigation.replace("Onboarding")
          }
        }
      ])
    } catch (e) {
      Alert.alert("Error", "An error occurred during sign up.")
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

