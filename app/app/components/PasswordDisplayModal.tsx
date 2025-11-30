import React, { useState } from "react"
import {
  View,
  Modal,
  TouchableOpacity,
  ViewStyle,
  TextStyle,
} from "react-native"
import { X, Eye, EyeOff, Key, Copy } from "lucide-react-native"
import { Text } from "./Text"

interface PasswordDisplayModalProps {
  visible: boolean
  onClose: () => void
  email: string
  password: string
}

export const PasswordDisplayModal: React.FC<PasswordDisplayModalProps> = ({
  visible,
  onClose,
  email,
  password,
}) => {
  const [showPassword, setShowPassword] = useState(false)

  const handleClose = () => {
    setShowPassword(false)
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
            <Key size={48} color="#f66c51" />
          </View>

          {/* Title */}
          <Text style={$title}>비밀번호 찾기 완료</Text>

          {/* Email info */}
          <View style={$infoSection}>
            <Text style={$infoLabel}>계정</Text>
            <View style={$infoValueContainer}>
              <Text style={$infoValue}>{email}</Text>
            </View>
          </View>

          {/* Password info */}
          <View style={$infoSection}>
            <Text style={$infoLabel}>비밀번호</Text>
            <View style={$passwordContainer}>
              <Text style={$passwordValue}>
                {showPassword ? password : "••••••••"}
              </Text>
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

          {/* Warning */}
          <View style={$warningContainer}>
            <Text style={$warningText}>
              보안을 위해 비밀번호를 확인한 후{"\n"}
              즉시 변경하시기 바랍니다.
            </Text>
          </View>

          {/* Button */}
          <TouchableOpacity style={$button} onPress={handleClose}>
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
  marginBottom: 24,
  textAlign: "center",
}

const $infoSection: ViewStyle = {
  width: "100%",
  marginBottom: 16,
}

const $infoLabel: TextStyle = {
  fontSize: 14,
  fontWeight: "500",
  color: "#1c100d",
  marginBottom: 8,
}

const $infoValueContainer: ViewStyle = {
  backgroundColor: "#f4e9e7",
  borderRadius: 12,
  paddingHorizontal: 16,
  paddingVertical: 12,
}

const $infoValue: TextStyle = {
  fontSize: 16,
  color: "#1c100d",
}

const $passwordContainer: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  backgroundColor: "#f4e9e7",
  borderRadius: 12,
  paddingHorizontal: 16,
  paddingVertical: 12,
}

const $passwordValue: TextStyle = {
  flex: 1,
  fontSize: 16,
  color: "#1c100d",
  fontWeight: "600",
  letterSpacing: 1,
}

const $eyeButton: ViewStyle = {
  padding: 4,
  marginLeft: 8,
}

const $warningContainer: ViewStyle = {
  backgroundColor: "#fff3e0",
  borderRadius: 8,
  padding: 12,
  marginTop: 8,
  marginBottom: 24,
  width: "100%",
}

const $warningText: TextStyle = {
  fontSize: 12,
  color: "#e65100",
  textAlign: "center",
  lineHeight: 18,
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