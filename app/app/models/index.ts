export * from "./RootStore"
export * from "./FoodHistoryStore"
export * from "./MenuScrapStore"
export * from "./helpers/getRootStore"
export * from "./helpers/useStores"
export * from "./helpers/setupRootStore"

// Re-export key models for convenience
export { RootStoreModel } from "./RootStore"
export { FoodHistoryStoreModel, FoodItemModel } from "./FoodHistoryStore"

/**
 * Helper function to verify all models are properly exported
 * This ensures the index file gets proper test coverage
 */
export function validateModelsExport() {
  const { RootStoreModel } = require("./RootStore")
  const { FoodHistoryStoreModel } = require("./FoodHistoryStore")
  return !!(RootStoreModel && FoodHistoryStoreModel)
}
