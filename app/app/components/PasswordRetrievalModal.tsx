import React, { useState } from "react"
import {
  View,
  Modal,
  TouchableOpacity,
  TextInput,
  ViewStyle,
  TextStyle,
} from "react-native"
import { X, Mail } from "lucide-react-native"
import { Text } from "./Text"

interface PasswordRetrievalModalProps {
  visible: boolean
  onClose: () => void
  onEmailSubmit: (email: string) => void
}

export const PasswordRetrievalModal: React.FC<PasswordRetrievalModalProps> = ({
  visible,
  onClose,
  onEmailSubmit,
}) => {
  const [email, setEmail] = useState("")

  const handleSubmit = () => {
    if (email.trim()) {
      onEmailSubmit(email.trim())
      setEmail("")
    }
  }

  const handleClose = () => {
    setEmail("")
    onClose()
  }

  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={handleClose}
    >
      <View style={$overlay}>
        <View style={$container}>
          {/* Close button */}
          <TouchableOpacity style={$closeButton} onPress={handleClose}>
            <X size={24} color="#9c5749" />
          </TouchableOpacity>

          {/* Icon */}
          <View style={$iconContainer}>
            <Mail size={48} color="#f66c51" />
          </View>

          {/* Title */}
          <Text style={$title}>비밀번호 찾기</Text>

          {/* Description */}
          <Text style={$description}>
            계정의 이메일 주소를 입력하시면{"\n"}
            인증 코드를 보내드립니다.
          </Text>

          {/* Email input */}
          <View style={$inputWrapper}>
            <Text style={$label}>이메일</Text>
            <TextInput
              style={$input}
              placeholder="이메일을 입력하세요"
              placeholderTextColor="#9c5749"
              value={email}
              onChangeText={setEmail}
              keyboardType="email-address"
              autoCapitalize="none"
              autoCorrect={false}
            />
          </View>

          {/* Buttons */}
          <View style={$buttonContainer}>
            <TouchableOpacity style={$cancelButton} onPress={handleClose}>
              <Text style={$cancelButtonText}>취소</Text>
            </TouchableOpacity>
            
            <TouchableOpacity 
              style={[$submitButton, !email.trim() && $submitButtonDisabled]} 
              onPress={handleSubmit}
              disabled={!email.trim()}
            >
              <Text style={$submitButtonText}>전송</Text>
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

const $input: TextStyle = {
  width: "100%",
  height: 48,
  backgroundColor: "#f4e9e7",
  borderRadius: 12,
  paddingHorizontal: 16,
  fontSize: 16,
  color: "#1c100d",
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

const $submitButton: ViewStyle = {
  flex: 1,
  height: 48,
  backgroundColor: "#f66c51",
  borderRadius: 12,
  justifyContent: "center",
  alignItems: "center",
}

const $submitButtonDisabled: ViewStyle = {
  opacity: 0.5,
}

const $submitButtonText: TextStyle = {
  fontSize: 16,
  fontWeight: "700",
  color: "#FFFFFF",
}