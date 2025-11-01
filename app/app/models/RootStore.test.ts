import { RootStoreModel } from "./RootStore"

describe("RootStoreModel", () => {
  it("should create with default food history store", () => {
    const rootStore = RootStoreModel.create()
    
    expect(rootStore.foodHistoryStore).toBeDefined()
    expect(rootStore.foodHistoryStore.scrappedItems.length).toBe(0)
  })

  it("should create with provided food history store data", () => {
    const initialData = {
      foodHistoryStore: {
        scrappedItems: [
          {
            id: 1,
            name: "Test Food",
            distance: "1.0 km",
            image: "test.jpg",
            keywords: ["test"],
            category: "restaurant",
            allergens: ["gluten"]
          }
        ]
      }
    }

    const rootStore = RootStoreModel.create(initialData)
    
    expect(rootStore.foodHistoryStore.scrappedItems.length).toBe(1)
    expect(rootStore.foodHistoryStore.scrappedItems[0].name).toBe("Test Food")
  })

  it("should allow access to food history store methods", () => {
    const rootStore = RootStoreModel.create()
    const testItem = {
      id: 1,
      name: "Pizza",
      distance: "1.2 km",
      image: "pizza.jpg",
      keywords: ["pizza"],
      category: "restaurant",
      allergens: ["gluten"]
    }

    expect(rootStore.foodHistoryStore.isScrapped(1)).toBe(false)
    
    rootStore.foodHistoryStore.addScrappedItem(testItem)
    
    expect(rootStore.foodHistoryStore.isScrapped(1)).toBe(true)
    expect(rootStore.foodHistoryStore.scrappedItems.length).toBe(1)
  })
})