import React, { useState } from "react"
import {
  View,
  Modal,
  TouchableOpacity,
  TextInput,
  ViewStyle,
  TextStyle,
} from "react-native"
import { X, Lock, Eye, EyeOff } from "lucide-react-native"
import { Text } from "./Text"

interface PasswordConfirmationModalProps {
  visible: boolean
  onCancel: () => void
  onConfirm: (password: string) => void
}

export const PasswordConfirmationModal: React.FC<PasswordConfirmationModalProps> = ({
  visible,
  onCancel,
  onConfirm,
}) => {
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)

  const handleConfirm = () => {
    if (password.trim()) {
      onConfirm(password.trim())
      setPassword("")
      setShowPassword(false)
    }
  }

  const handleCancel = () => {
    setPassword("")
    setShowPassword(false)
    onCancel()
  }

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={handleCancel}
    >
      <View style={$overlay}>
        <View style={$container}>
          {/* Close button */}
          <TouchableOpacity style={$closeButton} onPress={handleCancel}>
            <X size={24} color="#9c5749" />
          </TouchableOpacity>

          {/* Lock icon */}
          <View style={$iconContainer}>
            <Lock size={48} color="#f66c51" />
          </View>

          {/* Title */}
          <Text style={$title}>비밀번호 확인</Text>

          {/* Description */}
          <Text style={$description}>
            계정을 탈퇴하려면{"\n"}
            현재 비밀번호를 입력해주세요.
          </Text>

          {/* Password input */}
          <View style={$inputWrapper}>
            <Text style={$label}>비밀번호</Text>
            <View style={$passwordContainer}>
              <TextInput
                style={$passwordInput}
                placeholder="비밀번호를 입력하세요"
                placeholderTextColor="#9c5749"
                secureTextEntry={!showPassword}
                value={password}
                onChangeText={setPassword}
                autoCorrect={false}
                autoCapitalize="none"
              />
              <TouchableOpacity 
                style={$eyeButton}
                onPress={() => setShowPassword(!showPassword)}
              >
                {showPassword ? (
                  <Eye size={20} color="#9c5749" />
                ) : (
                  <EyeOff size={20} color="#9c5749" />
                )}
              </TouchableOpacity>
            </View>
          </View>

          {/* Buttons */}
          <View style={$buttonContainer}>
            <TouchableOpacity style={$cancelButton} onPress={handleCancel}>
              <Text style={$cancelButtonText}>취소</Text>
            </TouchableOpacity>
            
            <TouchableOpacity 
              style={[$confirmButton, !password.trim() && $confirmButtonDisabled]} 
              onPress={handleConfirm}
              disabled={!password.trim()}
            >
              <Text style={$confirmButtonText}>확인</Text>
            </TouchableOpacity>
          </View>
        </View>
      </View>
    </Modal>
  )
}

const $overlay: ViewStyle = {
  flex: 1,
  backgroundColor: "rgba(0, 0, 0, 0.5)",
  justifyContent: "center",
  alignItems: "center",
  paddingHorizontal: 20,
}

const $container: ViewStyle = {
  width: "100%",
  maxWidth: 360,
  backgroundColor: "#f8f6f5",
  borderRadius: 16,
  padding: 24,
  alignItems: "center",
  elevation: 8,
  shadowColor: "#000",
  shadowOffset: {
    width: 0,
    height: 4,
  },
  shadowOpacity: 0.25,
  shadowRadius: 8,
}

const $closeButton: ViewStyle = {
  position: "absolute",
  top: 16,
  right: 16,
  padding: 4,
  zIndex: 1,
}

const $iconContainer: ViewStyle = {
  marginBottom: 16,
  marginTop: 8,
}

const $title: TextStyle = {
  fontSize: 20,
  fontWeight: "700",
  color: "#1c100d",
  marginBottom: 8,
  textAlign: "center",
}

const $description: TextStyle = {
  fontSize: 14,
  color: "#9c5749",
  textAlign: "center",
  lineHeight: 20,
  marginBottom: 24,
}

const $inputWrapper: ViewStyle = {
  width: "100%",
  marginBottom: 24,
}

const $label: TextStyle = {
  fontSize: 14,
  fontWeight: "500",
  color: "#1c100d",
  marginBottom: 8,
}

const $passwordContainer: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  backgroundColor: "#f4e9e7",
  borderRadius: 12,
  height: 48,
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

const $buttonContainer: ViewStyle = {
  flexDirection: "row",
  gap: 12,
  width: "100%",
}

const $cancelButton: ViewStyle = {
  flex: 1,
  height: 48,
  backgroundColor: "#f4e9e7",
  borderRadius: 12,
  justifyContent: "center",
  alignItems: "center",
}

const $cancelButtonText: TextStyle = {
  fontSize: 16,
  fontWeight: "600",
  color: "#9c5749",
}

const $confirmButton: ViewStyle = {
  flex: 1,
  height: 48,
  backgroundColor: "#f66c51",
  borderRadius: 12,
  justifyContent: "center",
  alignItems: "center",
}

const $confirmButtonDisabled: ViewStyle = {
  opacity: 0.5,
}

const $confirmButtonText: TextStyle = {
  fontSize: 16,
  fontWeight: "700",
  color: "#FFFFFF",
}