import React from "react"
import {
  View,
  Modal,
  TouchableOpacity,
  ViewStyle,
  TextStyle,
} from "react-native"
import { X, MapPin } from "lucide-react-native"
import { Text } from "./Text"

interface LocationWarningModalProps {
  visible: boolean
  onCancel: () => void
  onConfirm: () => void
}

export const LocationWarningModal: React.FC<LocationWarningModalProps> = ({
  visible,
  onCancel,
  onConfirm,
}) => {
  return (
    <Modal
      visible={visible}
      transparent
      animationType="fade"
      onRequestClose={onCancel}
    >
      <View style={$overlay}>
        <View style={$container}>
          {/* Close button */}
          <TouchableOpacity style={$closeButton} onPress={onCancel}>
            <X size={24} color="#9c5749" />
          </TouchableOpacity>

          {/* Warning icon */}
          <View style={$iconContainer}>
            <MapPin size={48} color="#f66c51" />
          </View>

          {/* Title */}
          <Text style={$title}>위치 서비스 비활성화</Text>

          {/* Message */}
          <Text style={$message}>
            위치 서비스를 활성화하지 않을 경우,{"\n"}
            위치 기반 음식 추천을 받을 수 없습니다.
          </Text>

          {/* Buttons */}
          <View style={$buttonContainer}>
            <TouchableOpacity style={$cancelButton} onPress={onCancel}>
              <Text style={$cancelButtonText}>취소</Text>
            </TouchableOpacity>
            
            <TouchableOpacity style={$confirmButton} onPress={onConfirm}>
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

const $confirmButtonText: TextStyle = {
  fontSize: 16,
  fontWeight: "700",
  color: "#FFFFFF",
}