import { types } from "mobx-state-tree"
import { withSetPropAction } from "./withSetPropAction"

describe("withSetPropAction", () => {
  it("should allow setting properties with type safety", () => {
    const TestModel = types
      .model("TestModel")
      .props({
        name: types.string,
        age: types.number,
        active: types.boolean
      })
      .actions(withSetPropAction)

    const instance = TestModel.create({
      name: "John",
      age: 25,
      active: true
    })

    expect(instance.name).toBe("John")
    expect(instance.age).toBe(25)
    expect(instance.active).toBe(true)

    // Test setting string property
    instance.setProp("name", "Jane")
    expect(instance.name).toBe("Jane")

    // Test setting number property
    instance.setProp("age", 30)
    expect(instance.age).toBe(30)

    // Test setting boolean property
    instance.setProp("active", false)
    expect(instance.active).toBe(false)
  })

  it("should work with optional properties", () => {
    const TestModelWithOptional = types
      .model("TestModelWithOptional")
      .props({
        required: types.string,
        optional: types.maybe(types.string)
      })
      .actions(withSetPropAction)

    const instance = TestModelWithOptional.create({
      required: "test"
    })

    expect(instance.required).toBe("test")
    expect(instance.optional).toBeUndefined()

    instance.setProp("optional", "now set")
    expect(instance.optional).toBe("now set")

    instance.setProp("optional", undefined)
    expect(instance.optional).toBeUndefined()
  })

  it("should work with array properties", () => {
    const TestModelWithArray = types
      .model("TestModelWithArray")
      .props({
        items: types.array(types.string),
        numbers: types.array(types.number)
      })
      .actions(withSetPropAction)

    const instance = TestModelWithArray.create({
      items: ["item1", "item2"],
      numbers: [1, 2, 3]
    })

    expect(instance.items.length).toBe(2)
    expect(instance.numbers.length).toBe(3)

    instance.setProp("items", ["new1", "new2", "new3"])
    expect(instance.items.length).toBe(3)
    expect(instance.items[0]).toBe("new1")

    instance.setProp("numbers", [10, 20])
    expect(instance.numbers.length).toBe(2)
    expect(instance.numbers[0]).toBe(10)
  })

  it("should work with nested model properties", () => {
    const NestedModel = types.model("Nested").props({
      value: types.string
    })

    const ParentModel = types
      .model("Parent")
      .props({
        name: types.string,
        nested: NestedModel
      })
      .actions(withSetPropAction)

    const instance = ParentModel.create({
      name: "parent",
      nested: { value: "nested-value" }
    })

    expect(instance.name).toBe("parent")
    expect(instance.nested.value).toBe("nested-value")

    instance.setProp("name", "new-parent")
    expect(instance.name).toBe("new-parent")

    instance.setProp("nested", { value: "new-nested-value" })
    expect(instance.nested.value).toBe("new-nested-value")
  })

  it("should maintain reactivity after setting properties", () => {
    const ReactiveModel = types
      .model("ReactiveModel")
      .props({
        count: types.number
      })
      .actions(withSetPropAction)
      .views((self) => ({
        get doubled() {
          return self.count * 2
        }
      }))

    const instance = ReactiveModel.create({ count: 5 })

    expect(instance.count).toBe(5)
    expect(instance.doubled).toBe(10)

    instance.setProp("count", 10)

    expect(instance.count).toBe(10)
    expect(instance.doubled).toBe(20)
  })
})