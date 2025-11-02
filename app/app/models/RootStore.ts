import { Instance, SnapshotOut, types } from "mobx-state-tree"
import { FoodHistoryStoreModel } from "./FoodHistoryStore"
import { MenuScrapStoreModel } from "./MenuScrapStore"

/**
 * A RootStore model.
 */
export const RootStoreModel = types.model("RootStore").props({
  foodHistoryStore: types.optional(FoodHistoryStoreModel, {}),
  menuScrapStore: types.optional(MenuScrapStoreModel, {}),
})

/**
 * The RootStore instance.
 */
export interface RootStore extends Instance<typeof RootStoreModel> {}
/**
 * The data of a RootStore.
 */
export interface RootStoreSnapshot extends SnapshotOut<typeof RootStoreModel> {}
