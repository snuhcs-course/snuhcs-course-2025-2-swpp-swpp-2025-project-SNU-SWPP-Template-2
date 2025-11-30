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

interface AccountDeletionErrorModalProps {
  visible: boolean
  onClose: () => void
  errorMessage?: string
}

export const AccountDeletionErrorModal: React.FC<AccountDeletionErrorModalProps> = ({
  visible,
  onClose,
  errorMessage,
}) => {
  // Convert error messages to user-friendly Korean
  const getErrorMessage = (message?: string) => {
    if (!message) return "계정 삭제에 실패했습니다."
    
    const lowerMessage = message.toLowerCase()
    
    if (lowerMessage.includes("invalid password") || lowerMessage.includes("비밀번호")) {
      return "비밀번호가 올바르지 않습니다."
    } else if (lowerMessage.includes("aws") && lowerMessage.includes("삭제")) {
      return "AWS 계정 삭제에 실패했습니다. 다시 시도해주세요."
    } else if (lowerMessage.includes("삭제 중 오류") || lowerMessage.includes("deletion error")) {
      return "계정 삭제 중 오류가 발생했습니다."
    } else if (lowerMessage.includes("network") || lowerMessage.includes("connection")) {
      return "네트워크 연결을 확인해주세요."
    } else {
      return message // Return the original message if it's already in Korean
    }
  }

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={() => {}} // Prevent Android back button from closing
    >
      <TouchableOpacity 
        style={$overlay}
        activeOpacity={1}
        onPress={() => {}} // Prevent closing when tapping overlay
      >
        <TouchableOpacity 
          style={$container}
          activeOpacity={1}
          onPress={() => {}} // Prevent closing when tapping modal content
        >
          {/* Close button */}
          <TouchableOpacity style={$closeButton} onPress={onClose}>
            <X size={24} color="#9c5749" />
          </TouchableOpacity>

          {/* Error icon */}
          <View style={$iconContainer}>
            <AlertCircle size={48} color="#f66c51" />
          </View>

          {/* Title */}
          <Text style={$title}>계정 삭제 실패</Text>

          {/* Error message */}
          <Text style={$message}>
            {getErrorMessage(errorMessage)}
          </Text>

          {/* Action button */}
          <TouchableOpacity style={$button} onPress={onClose}>
            <Text style={$buttonText}>확인</Text>
          </TouchableOpacity>
        </TouchableOpacity>
      </TouchableOpacity>
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