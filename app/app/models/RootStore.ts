import { Instance, SnapshotOut, types } from "mobx-state-tree"
import { FoodHistoryStoreModel } from "./FoodHistoryStore"
import { MenuScrapStoreModel } from "./MenuScrapStore"

/**
 * A RootStore model.
 */
export const RootStoreModel = types.model("RootStore").props({
  foodHistoryStore: types.optional(FoodHistoryStoreModel, {}),
  menuScrapStore: types.optional(MenuScrapStoreModel, {}),
  _isLoading: types.optional(types.boolean, false),
}).actions((self) => ({
  async clearUserData() {
    // Clear user-specific data when user logs out (but preserve scraped menus)
    console.log("🧹 DEBUG: clearUserData() called - clearing stores but preserving scraped menus")
    console.trace("🧹 STACK TRACE: Where clearUserData was called from:")
    self.foodHistoryStore.clearAllScraps()
    // Note: NOT clearing menuScrapStore to preserve scraped menus across sessions
    
    // Selectively preserve scraped menus while clearing other user data
    try {
      const storage = require("../utils/storage")
      
      // Backup scraped menus before clearing storage
      const currentState = await storage.load("root-v1")
      let scraped_menus_backup = null
      if (currentState?.menuScrapStore?.scrappedMenus) {
        scraped_menus_backup = currentState.menuScrapStore.scrappedMenus
        console.log(`💾 Backing up ${scraped_menus_backup.length} scraped menus before logout`)
      }
      
      // Clear the persisted state from AsyncStorage  
      await storage.remove("root-v1") // This key must match ROOT_STATE_STORAGE_KEY in setupRootStore.ts
      await storage.save("user-logged-out", "true") // Flag to prevent restoring stale data
      
      // Restore scraped menus after clearing
      if (scraped_menus_backup && scraped_menus_backup.length > 0) {
        await storage.save("preserved-scraped-menus", scraped_menus_backup)
        console.log(`💾 Preserved ${scraped_menus_backup.length} scraped menus in separate storage`)
      }
      
      console.log("🧹 Cleared persisted state from AsyncStorage but preserved scraped menus")
    } catch (error) {
      console.warn("Failed to selectively clear persisted state:", error)
    }
  },
  
  async loadUserDataFromBackend() {
    // Load user-specific data from backend when user logs in
    console.log("🔄 loadUserDataFromBackend() called - starting data load process")
    
    // Prevent multiple concurrent calls
    if (self._isLoading) {
      console.log("⏸️ Already loading user data, skipping duplicate call")
      return
    }
    self._isLoading = true
    
    try {
      const { api } = require("../services/api")
      
      // Load and merge backend data with existing scraped menus (don't clear)
      console.log("🔄 Loading backend scraps and merging with existing local scraps")
      
      // Load scraps from backend
      const scrapsResponse = await api.getScraps()
      console.log("🔍 DEBUG: getScraps API response:", scrapsResponse)
      if (scrapsResponse.ok && scrapsResponse.data) {
        // Convert backend scraps to the format expected by both stores
        const scraps = scrapsResponse.data
        console.log("🔍 DEBUG: Scraps data from backend:", scraps)
        console.log(`📊 Processing ${scraps.length} scraps from backend`)
        
        let addedCount = 0
        for (const scrap of scraps) {
          if (scrap.restaurant) {
            const restaurant = scrap.restaurant
            console.log("🔍 DEBUG: Adding restaurant to stores:", restaurant.name, "ID:", restaurant.id)
            
            // Add to FoodHistoryStore format (for legacy compatibility)
            self.foodHistoryStore.addScrappedItem({
              id: restaurant.id,
              name: restaurant.name,
              distance: "0km", // Default distance
              image: restaurant.image_url && restaurant.image_url.trim() ? restaurant.image_url : "", // Empty string for placeholder
              keywords: [], // Default keywords
              category: "restaurant", // Default since backend doesn't have category
              allergens: [], // Default allergens
            })
            
            // Add to MenuScrapStore format (for menu-based scraps)
            self.menuScrapStore.addScrappedMenu({
              id: restaurant.id,
              menu_name: restaurant.name, // Use restaurant name as menu name for now
              place_name: restaurant.name,
              price: 0, // Show ₩0 instead of null for consistent display
              category: "restaurant", // Default since backend doesn't have category
              location: restaurant.address && restaurant.address.trim() ? restaurant.address : "위치 정보 없음",
              rating: 0, // Default rating since backend doesn't have rating
              review_count: 0, // Backend scraps don't have review count
              image_url: restaurant.image_url && restaurant.image_url.trim() ? restaurant.image_url : undefined,
              coordinates: [parseFloat(restaurant.longitude) || 0, parseFloat(restaurant.latitude) || 0],
            })
            addedCount++
          } else {
            console.warn("⚠️ Scrap missing restaurant data:", scrap)
          }
        }
        console.log(`✅ Successfully added ${addedCount} items to stores`)
      }
      
      // Clear the logout flag since user has successfully logged in and loaded data
      try {
        const storage = require("../utils/storage")
        await storage.remove("user-logged-out")
        console.log("✅ User data loaded successfully - cleared logout flag")
      } catch (error) {
        console.warn("Failed to clear logout flag:", error)
      }
    } catch (error) {
      console.error("Failed to load user data from backend:", error)
    } finally {
      self._isLoading = false
      console.log("🏁 loadUserDataFromBackend() completed")
    }
  },
}))

/**
 * The RootStore instance.
 */
export interface RootStore extends Instance<typeof RootStoreModel> {}
/**
 * The data of a RootStore.
 */
export interface RootStoreSnapshot extends SnapshotOut<typeof RootStoreModel> {}
