import React from "react"
import {
  View,
  Modal,
  TouchableOpacity,
  ViewStyle,
  TextStyle,
} from "react-native"
import { X, AlertCircle } from "lucide-react-native"
import { Text } from "./Text"

interface SignUpErrorModalProps {
  visible: boolean
  onClose: () => void
  errorMessage?: string
}

export const SignUpErrorModal: React.FC<SignUpErrorModalProps> = ({
  visible,
  onClose,
  errorMessage,
}) => {
  // Convert English error messages to Korean
  const getKoreanMessage = (message?: string) => {
    if (!message) return "회원가입에 실패했습니다."
    
    const lowerMessage = message.toLowerCase()
    
    if (lowerMessage.includes("username already exists") || lowerMessage.includes("이미 존재")) {
      return "이미 사용 중인 아이디입니다."
    } else if (lowerMessage.includes("email already exists") || lowerMessage.includes("이메일")) {
      return "이미 사용 중인 이메일입니다."
    } else if (lowerMessage.includes("aws") && lowerMessage.includes("회원가입")) {
      return "회원가입 중 인증 서비스 오류가 발생했습니다."
    } else if (lowerMessage.includes("password")) {
      return "비밀번호가 요구사항을 충족하지 않습니다."
    } else if (lowerMessage.includes("network") || lowerMessage.includes("connection")) {
      return "네트워크 연결을 확인해주세요."
    } else if (lowerMessage.includes("보안 토큰")) {
      return "보안 토큰을 가져오지 못했습니다."
    } else {
      return message // Return the original message if it's already in Korean
    }
  }

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onClose}
    >
      <View style={$overlay}>
        <View style={$container}>
          {/* Close button */}
          <TouchableOpacity style={$closeButton} onPress={onClose}>
            <X size={24} color="#9c5749" />
          </TouchableOpacity>

          {/* Error icon */}
          <View style={$iconContainer}>
            <AlertCircle size={48} color="#f66c51" />
          </View>

          {/* Title */}
          <Text style={$title}>회원가입 실패</Text>

          {/* Error message */}
          <Text style={$message}>
            {getKoreanMessage(errorMessage)}
          </Text>

          {/* Action button */}
          <TouchableOpacity style={$button} onPress={onClose}>
            <Text style={$buttonText}>확인</Text>
          </TouchableOpacity>
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
  maxWidth: 340,
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
  marginBottom: 12,
  textAlign: "center",
  lineHeight: 28, // Explicit line height to prevent cutoff
  paddingVertical: 4, // Small vertical padding to ensure text isn't clipped
}

const $message: TextStyle = {
  fontSize: 16,
  color: "#9c5749",
  textAlign: "center",
  lineHeight: 24,
  marginBottom: 24,
}

const $button: ViewStyle = {
  width: "100%",
  height: 48,
  backgroundColor: "#f66c51",
  borderRadius: 12,
  justifyContent: "center",
  alignItems: "center",
}

const $buttonText: TextStyle = {
  fontSize: 16,
  fontWeight: "700",
  color: "#FFFFFF",
}