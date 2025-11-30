import React, { useState } from "react"
import {
  View,
  Modal,
  TouchableOpacity,
  TextInput,
  ViewStyle,
  TextStyle,
} from "react-native"
import { X, Shield } from "lucide-react-native"
import { Text } from "./Text"

interface VerificationCodeModalProps {
  visible: boolean
  onClose: () => void
  onCodeVerify: (code: string) => void
  email: string
}

export const VerificationCodeModal: React.FC<VerificationCodeModalProps> = ({
  visible,
  onClose,
  onCodeVerify,
  email,
}) => {
  const [code, setCode] = useState("")
  const [error, setError] = useState("")

  const handleSubmit = () => {
    if (code.trim() === "123456") {
      onCodeVerify(code.trim())
      setCode("")
      setError("")
    } else {
      setError("인증 코드가 올바르지 않습니다.")
    }
  }

  const handleClose = () => {
    setCode("")
    setError("")
    onClose()
  }

  const handleCodeChange = (text: string) => {
    // Only allow numbers and limit to 6 digits
    const numericText = text.replace(/[^0-9]/g, '').slice(0, 6)
    setCode(numericText)
    if (error) setError("")
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
            <Shield size={48} color="#f66c51" />
          </View>

          {/* Title */}
          <Text style={$title}>인증 코드 입력</Text>

          {/* Description */}
          <Text style={$description}>
            {email}로{"\n"}
            인증 코드를 보내드렸습니다.{"\n"}
            코드를 입력해주세요.
          </Text>

          {/* Code input */}
          <View style={$inputWrapper}>
            <Text style={$label}>인증 코드</Text>
            <TextInput
              style={[$input, error && $inputError]}
              placeholder="123456"
              placeholderTextColor="#9c5749"
              value={code}
              onChangeText={handleCodeChange}
              keyboardType="numeric"
              maxLength={6}
              textAlign="center"
              fontSize={18}
              letterSpacing={2}
            />
            {error ? <Text style={$errorText}>{error}</Text> : null}
          </View>

          {/* Hint */}
          <Text style={$hint}>
            개발 버전에서는 123456을 입력하세요
          </Text>

          {/* Buttons */}
          <View style={$buttonContainer}>
            <TouchableOpacity style={$cancelButton} onPress={handleClose}>
              <Text style={$cancelButtonText}>취소</Text>
            </TouchableOpacity>
            
            <TouchableOpacity 
              style={[$submitButton, code.length !== 6 && $submitButtonDisabled]} 
              onPress={handleSubmit}
              disabled={code.length !== 6}
            >
              <Text style={$submitButtonText}>확인</Text>
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
  marginBottom: 20,
}

const $inputWrapper: ViewStyle = {
  width: "100%",
  marginBottom: 16,
}

const $label: TextStyle = {
  fontSize: 14,
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
  fontSize: 18,
  color: "#1c100d",
  fontWeight: "600",
}

const $inputError: ViewStyle = {
  borderWidth: 1,
  borderColor: "#f66c51",
}

const $errorText: TextStyle = {
  fontSize: 12,
  color: "#f66c51",
  marginTop: 4,
}

const $hint: TextStyle = {
  fontSize: 12,
  color: "#9c5749",
  textAlign: "center",
  marginBottom: 20,
  fontStyle: "italic",
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