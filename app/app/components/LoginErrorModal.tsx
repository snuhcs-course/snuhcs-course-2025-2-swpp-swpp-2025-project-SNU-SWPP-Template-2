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

interface LoginErrorModalProps {
  visible: boolean
  onClose: () => void
  errorMessage?: string
}

export const LoginErrorModal: React.FC<LoginErrorModalProps> = ({
  visible,
  onClose,
  errorMessage
}) => {
  const defaultMessage = "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요."

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
          <Text style={$title}>로그인 오류</Text>

          {/* Error message */}
          <Text style={$message}>
            {errorMessage || defaultMessage}
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