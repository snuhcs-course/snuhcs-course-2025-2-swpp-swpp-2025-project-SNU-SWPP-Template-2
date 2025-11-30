import React, { useState, useEffect } from "react"
import {
  View,
  Modal,
  TouchableOpacity,
  Switch,
  ViewStyle,
  TextStyle,
} from "react-native"
import { X, MapPin, Image, LogOut } from "lucide-react-native"
import { Text } from "./Text"
import { LocationWarningModal, StorageWarningModal } from "./index"
import * as Location from "expo-location"
import * as MediaLibrary from "expo-media-library"
import * as storage from "app/utils/storage"

interface SettingsModalProps {
  visible: boolean
  onClose: () => void
  onLogout: () => void
  onDeleteAccount: () => void
}

export const SettingsModal: React.FC<SettingsModalProps> = ({
  visible,
  onClose,
  onLogout,
  onDeleteAccount,
}) => {
  const [locationEnabled, setLocationEnabled] = useState(false)
  const [storageEnabled, setStorageEnabled] = useState(false)
  const [locationWarningVisible, setLocationWarningVisible] = useState(false)
  const [storageWarningVisible, setStorageWarningVisible] = useState(false)
  
  // Use the same hook as OnboardingScreen for photo permissions
  const [galleryPermissionResponse, requestGalleryPermission] = MediaLibrary.usePermissions(
    { granularPermissions: ['photo'] }
  )

  // Check current permission status when modal opens
  useEffect(() => {
    if (visible) {
      checkPermissions()
    }
  }, [visible])

  // Note: We don't automatically sync with gallery permission hook response
  // because we want to preserve onboarding choices. The checkPermissions function
  // handles the proper logic for setting storage state based on onboarding results.

  const checkPermissions = async () => {
    try {
      // First check stored onboarding states
      const storedLocationGranted = await storage.loadString("LOCATION_PERMISSION_GRANTED")
      const storedGalleryGranted = await storage.loadString("GALLERY_PERMISSION_GRANTED")
      const useDummyLocation = await storage.loadString("USE_DUMMY_LOCATION")
      const useDummyStorage = await storage.loadString("USE_DUMMY_STORAGE")
      
      if (__DEV__) {
        console.log("Settings Modal - Stored onboarding values:", {
          storedLocationGranted,
          storedGalleryGranted,
          useDummyLocation,
          useDummyStorage,
        })
      }
      
      // Check current system permissions
      const locationStatus = await Location.getForegroundPermissionsAsync()
      const currentLocationGranted = locationStatus.status === 'granted'
      const currentGalleryGranted = galleryPermissionResponse?.status === 'granted'
      
      // Use stored onboarding state if available, otherwise fall back to system permissions
      if (storedLocationGranted !== null) {
        // If user chose to use dummy location in onboarding, disable the toggle
        const finalLocationEnabled = storedLocationGranted === 'true' && useDummyLocation !== 'true'
        setLocationEnabled(finalLocationEnabled)
        
        if (__DEV__) {
          console.log("Settings Modal - Location enabled:", finalLocationEnabled)
        }
      } else {
        setLocationEnabled(currentLocationGranted)
        if (__DEV__) {
          console.log("Settings Modal - Using system location permission:", currentLocationGranted)
        }
      }
      
      if (storedGalleryGranted !== null) {
        // If user chose to use dummy storage in onboarding, disable the toggle
        const finalStorageEnabled = storedGalleryGranted === 'true' && useDummyStorage !== 'true'
        setStorageEnabled(finalStorageEnabled)
        
        if (__DEV__) {
          console.log("Settings Modal - Storage enabled:", finalStorageEnabled)
        }
      } else {
        setStorageEnabled(currentGalleryGranted)
        if (__DEV__) {
          console.log("Settings Modal - Using system storage permission:", currentGalleryGranted)
        }
      }
    } catch (error) {
      console.log("Error checking permissions:", error)
    }
  }

  const handleLocationToggle = async (value: boolean) => {
    if (value) {
      // User wants to enable location
      const { status } = await Location.requestForegroundPermissionsAsync()
      const granted = status === 'granted'
      setLocationEnabled(granted)
      
      // Update stored state
      await storage.saveString("LOCATION_PERMISSION_GRANTED", granted.toString())
      await storage.saveString("USE_DUMMY_LOCATION", "false")
    } else {
      // User wants to disable location - show warning
      setLocationWarningVisible(true)
    }
  }

  const handleStorageToggle = async (value: boolean) => {
    if (value) {
      // User wants to enable storage - use the same hook as OnboardingScreen
      const response = await requestGalleryPermission()
      const granted = response?.status === 'granted'
      setStorageEnabled(granted)
      
      // Update stored state
      await storage.saveString("GALLERY_PERMISSION_GRANTED", granted.toString())
      await storage.saveString("USE_DUMMY_STORAGE", "false")
    } else {
      // User wants to disable storage - show warning
      setStorageWarningVisible(true)
    }
  }

  const handleLocationWarningConfirm = async () => {
    setLocationWarningVisible(false)
    setLocationEnabled(false)
    
    // Save the disabled state to storage
    await storage.saveString("LOCATION_PERMISSION_GRANTED", "false")
    await storage.saveString("USE_DUMMY_LOCATION", "true")
    
    if (__DEV__) {
      console.log("Settings Modal - Location disabled and saved to storage")
    }
  }

  const handleLocationWarningCancel = () => {
    setLocationWarningVisible(false)
    // Keep location enabled
  }

  const handleStorageWarningConfirm = async () => {
    setStorageWarningVisible(false)
    setStorageEnabled(false)
    
    // Save the disabled state to storage
    await storage.saveString("GALLERY_PERMISSION_GRANTED", "false")
    await storage.saveString("USE_DUMMY_STORAGE", "true")
    
    if (__DEV__) {
      console.log("Settings Modal - Storage disabled and saved to storage")
    }
  }

  const handleStorageWarningCancel = () => {
    setStorageWarningVisible(false)
    // Keep storage enabled
  }

  return (
    <>
      <Modal
        visible={visible}
        transparent
        animationType="fade"
        onRequestClose={onClose}
      >
        <View style={$overlay}>
          <View style={$container}>
            {/* Header */}
            <View style={$header}>
              <Text style={$title}>설정</Text>
              <TouchableOpacity style={$closeButton} onPress={onClose}>
                <X size={24} color="#9c5749" />
              </TouchableOpacity>
            </View>

            {/* Location Setting */}
            <View style={$settingItem}>
              <View style={$settingInfo}>
                <View style={$settingIcon}>
                  <MapPin size={20} color="#f66c51" />
                </View>
                <View style={$settingTextContainer}>
                  <Text style={$settingTitle}>위치 서비스</Text>
                  <Text style={$settingDescription}>
                    위치 기반 음식 추천
                  </Text>
                </View>
              </View>
              <Switch
                value={locationEnabled}
                onValueChange={handleLocationToggle}
                trackColor={{ false: "#f4e9e7", true: "#f66c51" }}
                thumbColor={locationEnabled ? "#ffffff" : "#9c5749"}
              />
            </View>

            {/* Storage Setting */}
            <View style={$settingItem}>
              <View style={$settingInfo}>
                <View style={$settingIcon}>
                  <Image size={20} color="#f66c51" />
                </View>
                <View style={$settingTextContainer}>
                  <Text style={$settingTitle}>갤러리 접근</Text>
                  <Text style={$settingDescription}>
                    음식 사진 기반 맞춤 추천
                  </Text>
                </View>
              </View>
              <Switch
                value={storageEnabled}
                onValueChange={handleStorageToggle}
                trackColor={{ false: "#f4e9e7", true: "#f66c51" }}
                thumbColor={storageEnabled ? "#ffffff" : "#9c5749"}
              />
            </View>

            {/* Divider */}
            <View style={$divider} />

            {/* Logout Button */}
            <TouchableOpacity style={$logoutButton} onPress={onLogout}>
              <LogOut size={20} color="#FFFFFF" />
              <Text style={$logoutText}>로그아웃</Text>
            </TouchableOpacity>

            {/* Delete Account Button */}
            <TouchableOpacity style={$deleteButton} onPress={onDeleteAccount}>
              <Text style={$deleteText}>탈퇴하기</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>

      {/* Warning Modals */}
      <LocationWarningModal
        visible={locationWarningVisible}
        onCancel={handleLocationWarningCancel}
        onConfirm={handleLocationWarningConfirm}
      />

      <StorageWarningModal
        visible={storageWarningVisible}
        onCancel={handleStorageWarningCancel}
        onConfirm={handleStorageWarningConfirm}
      />
    </>
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
  maxWidth: 400,
  backgroundColor: "#f8f6f5",
  borderRadius: 16,
  paddingHorizontal: 24,
  paddingTop: 24,
  paddingBottom: 24,
  elevation: 8,
  shadowColor: "#000",
  shadowOffset: {
    width: 0,
    height: 4,
  },
  shadowOpacity: 0.25,
  shadowRadius: 8,
}

const $header: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "flex-start", // Align to flex-start instead of center
  marginBottom: 24,
  minHeight: 32, // Ensure minimum height for the header
}

const $title: TextStyle = {
  fontSize: 24,
  fontWeight: "700",
  color: "#1c100d",
  lineHeight: 32, // Explicit line height to prevent cutoff
  paddingVertical: 4, // Small vertical padding to ensure text isn't clipped
}

const $closeButton: ViewStyle = {
  padding: 4,
  marginTop: 0, // Reset margin
}

const $settingItem: ViewStyle = {
  flexDirection: "row",
  justifyContent: "space-between",
  alignItems: "center",
  paddingVertical: 16,
  paddingHorizontal: 4,
}

const $settingInfo: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  flex: 1,
}

const $settingIcon: ViewStyle = {
  width: 40,
  height: 40,
  borderRadius: 20,
  backgroundColor: "#f4e9e7",
  justifyContent: "center",
  alignItems: "center",
  marginRight: 16,
}

const $settingTextContainer: ViewStyle = {
  flex: 1,
}

const $settingTitle: TextStyle = {
  fontSize: 16,
  fontWeight: "600",
  color: "#1c100d",
  marginBottom: 2,
}

const $settingDescription: TextStyle = {
  fontSize: 14,
  color: "#9c5749",
}

const $divider: ViewStyle = {
  height: 1,
  backgroundColor: "#f4e9e7",
  marginVertical: 16,
}

const $logoutButton: ViewStyle = {
  flexDirection: "row",
  alignItems: "center",
  justifyContent: "center",
  paddingVertical: 16,
  paddingHorizontal: 24,
  backgroundColor: "#f66c51", // Vivid orange concept color
  borderRadius: 12,
  gap: 8,
}

const $logoutText: TextStyle = {
  fontSize: 16,
  fontWeight: "600",
  color: "#FFFFFF", // White text
}

const $deleteButton: ViewStyle = {
  paddingVertical: 16,
  paddingHorizontal: 24,
  backgroundColor: "transparent",
  borderRadius: 12,
  alignItems: "center",
  justifyContent: "center",
  marginTop: 8,
}

const $deleteText: TextStyle = {
  fontSize: 14,
  fontWeight: "500",
  color: "#9c5749",
  textDecorationLine: "underline",
}