import React from "react"
import {
  View,
  Modal,
  TouchableOpacity,
  ViewStyle,
  TextStyle,
} from "react-native"
import { CheckCircle } from "lucide-react-native"
import { Text } from "./Text"

interface AccountDeletionSuccessModalProps {
  visible: boolean
  onConfirm: () => void
}

export const AccountDeletionSuccessModal: React.FC<AccountDeletionSuccessModalProps> = ({
  visible,
  onConfirm,
}) => {
  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onConfirm}
    >
      <View style={$overlay}>
        <View style={$container}>
          {/* Success icon */}
          <View style={$iconContainer}>
            <CheckCircle size={64} color="#4CAF50" />
          </View>

          {/* Title */}
          <Text style={$title}>탈퇴가 완료되었습니다.</Text>

          {/* Message */}
          <Text style={$message}>
            그동안 서비스를 이용해주셔서{"\n"}
            감사했습니다.
          </Text>

          {/* Action button */}
          <TouchableOpacity style={$button} onPress={onConfirm}>
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

const $iconContainer: ViewStyle = {
  marginBottom: 20,
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
  marginBottom: 32,
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