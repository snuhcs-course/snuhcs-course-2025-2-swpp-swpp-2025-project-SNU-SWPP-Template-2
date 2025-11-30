/**
 * This file is where we do "rehydration" of your RootStore from AsyncStorage.
 * This lets you persist your state between app launches.
 *
 * Navigation state persistence is handled in navigationUtilities.tsx.
 *
 * Note that Fast Refresh doesn't play well with this file, so if you edit this,
 * do a full refresh of your app instead.
 *
 * @refresh reset
 */
import { applySnapshot, IDisposer, onSnapshot } from "mobx-state-tree"
import { RootStore, RootStoreSnapshot } from "../RootStore"
import * as storage from "../../utils/storage"

/**
 * The key we'll be saving our state as within async storage.
 */
const ROOT_STATE_STORAGE_KEY = "root-v1"

/**
 * Setup the root state.
 */
let _disposer: IDisposer | undefined
export async function setupRootStore(rootStore: RootStore) {
  let restoredState: RootStoreSnapshot | undefined | null

  try {
    // Check if user logged out - if so, skip restoring old data but preserve scraped menus
    const userLoggedOut = await storage.load("user-logged-out")
    if (userLoggedOut === "true") {
      console.log("🚫 User logged out detected - skipping AsyncStorage restore to prevent data leakage")
      await storage.remove("user-logged-out") // Clear the flag
      
      // Restore preserved scraped menus even after logout
      try {
        const preservedMenus = await storage.load("preserved-scraped-menus")
        if (preservedMenus && Array.isArray(preservedMenus) && preservedMenus.length > 0) {
          console.log(`🔄 Restoring ${preservedMenus.length} preserved scraped menus after logout`)
          
          // Apply preserved scraped menus to the store
          for (const menu of preservedMenus) {
            rootStore.menuScrapStore.addScrappedMenu({
              id: menu.id,
              menu_name: menu.menu_name,
              place_name: menu.place_name,
              price: menu.price,
              category: menu.category,
              location: menu.location,
              rating: menu.rating,
              review_count: menu.review_count,
              image_url: menu.image_url,
              coordinates: menu.coordinates,
            })
          }
          
          // Clean up the temporary storage
          await storage.remove("preserved-scraped-menus")
          console.log("✅ Successfully restored and cleaned up preserved scraped menus")
        }
      } catch (error) {
        console.warn("Failed to restore preserved scraped menus:", error)
      }
    } else {
      // load the last known state from AsyncStorage
      restoredState = ((await storage.load(ROOT_STATE_STORAGE_KEY)) ?? {}) as RootStoreSnapshot
      applySnapshot(rootStore, restoredState)
      console.log("📂 Restored state from AsyncStorage")
    }
  } catch (e) {
    // if there's any problems loading, then inform the dev what happened
    if (__DEV__) {
      if (e instanceof Error) console.error(e.message)
    }
  }

  // stop tracking state changes if we've already setup
  if (_disposer) _disposer()

  // track changes & save to AsyncStorage
  _disposer = onSnapshot(rootStore, (snapshot) => storage.save(ROOT_STATE_STORAGE_KEY, snapshot))

  const unsubscribe = () => {
    _disposer?.()
    _disposer = undefined
  }

  return { rootStore, restoredState, unsubscribe }
}
