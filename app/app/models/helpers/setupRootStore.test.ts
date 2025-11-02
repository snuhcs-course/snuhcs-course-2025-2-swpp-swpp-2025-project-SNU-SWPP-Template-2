import { RootStoreModel } from "../RootStore"
import { setupRootStore } from "./setupRootStore"
import * as storage from "../../utils/storage"

// Mock the storage module
jest.mock("../../utils/storage", () => ({
  load: jest.fn(),
  save: jest.fn(),
}))

const mockStorage = storage as jest.Mocked<typeof storage>

describe("setupRootStore", () => {
  let rootStore: any

  beforeEach(() => {
    rootStore = RootStoreModel.create()
    jest.clearAllMocks()
    // Reset global dev flag for consistent testing
    global.__DEV__ = false
  })

  afterEach(() => {
    // Clean up any disposers that might be created
    jest.clearAllMocks()
  })

  it("should setup root store without restored state", async () => {
    mockStorage.load.mockResolvedValue(null)

    const result = await setupRootStore(rootStore)

    expect(result.rootStore).toBe(rootStore)
    expect(result.restoredState).toEqual({})
    expect(result.unsubscribe).toBeInstanceOf(Function)
    expect(mockStorage.load).toHaveBeenCalledWith("root-v1")
  })

  it("should restore state from storage", async () => {
    const savedState = {
      foodHistoryStore: {
        scrappedItems: [
          {
            id: 1,
            name: "Saved Pizza",
            distance: "1.0 km",
            image: "pizza.jpg",
            keywords: ["pizza"],
            category: "restaurant",
            allergens: ["gluten"]
          }
        ]
      }
    }

    mockStorage.load.mockResolvedValue(savedState)

    const result = await setupRootStore(rootStore)

    expect(result.restoredState).toEqual(savedState)
    expect(rootStore.foodHistoryStore.scrappedItems.length).toBe(1)
    expect(rootStore.foodHistoryStore.scrappedItems[0].name).toBe("Saved Pizza")
  })

  it("should handle storage load errors gracefully", async () => {
    const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation()
    global.__DEV__ = true
    
    mockStorage.load.mockRejectedValue(new Error("Storage error"))

    const result = await setupRootStore(rootStore)

    expect(result.rootStore).toBe(rootStore)
    expect(result.restoredState).toBeUndefined()
    expect(consoleErrorSpy).toHaveBeenCalledWith("Storage error")
    
    consoleErrorSpy.mockRestore()
  })

  it("should not log errors in production", async () => {
    const consoleErrorSpy = jest.spyOn(console, "error").mockImplementation()
    global.__DEV__ = false
    
    mockStorage.load.mockRejectedValue(new Error("Storage error"))

    const result = await setupRootStore(rootStore)

    expect(result.rootStore).toBe(rootStore)
    expect(consoleErrorSpy).not.toHaveBeenCalled()
    
    consoleErrorSpy.mockRestore()
  })

  it("should save snapshots when store changes", async () => {
    mockStorage.load.mockResolvedValue(null)

    const result = await setupRootStore(rootStore)

    // Make a change to trigger snapshot
    rootStore.foodHistoryStore.addScrappedItem({
      id: 1,
      name: "Test Food",
      distance: "1.0 km",
      image: "test.jpg",
      keywords: ["test"],
      category: "restaurant",
      allergens: []
    })

    // Wait for snapshot to be saved
    await new Promise(resolve => setTimeout(resolve, 0))

    expect(mockStorage.save).toHaveBeenCalledWith("root-v1", expect.objectContaining({
      foodHistoryStore: expect.objectContaining({
        scrappedItems: expect.arrayContaining([
          expect.objectContaining({
            id: 1,
            name: "Test Food"
          })
        ])
      })
    }))

    // Clean up
    result.unsubscribe()
  })

  it("should provide working unsubscribe function", async () => {
    mockStorage.load.mockResolvedValue(null)

    const result = await setupRootStore(rootStore)

    // Unsubscribe should not throw
    expect(() => result.unsubscribe()).not.toThrow()

    // After unsubscribe, changes should not trigger saves
    jest.clearAllMocks()
    
    rootStore.foodHistoryStore.addScrappedItem({
      id: 2,
      name: "Test Food 2",
      distance: "1.0 km",
      image: "test2.jpg",
      keywords: ["test"],
      category: "restaurant",
      allergens: []
    })

    await new Promise(resolve => setTimeout(resolve, 0))

    expect(mockStorage.save).not.toHaveBeenCalled()
  })

  it("should handle multiple setup calls correctly", async () => {
    mockStorage.load.mockResolvedValue(null)

    const result1 = await setupRootStore(rootStore)
    const result2 = await setupRootStore(rootStore)

    expect(result1.rootStore).toBe(rootStore)
    expect(result2.rootStore).toBe(rootStore)

    // Both should have valid unsubscribe functions
    expect(() => result1.unsubscribe()).not.toThrow()
    expect(() => result2.unsubscribe()).not.toThrow()
  })
})