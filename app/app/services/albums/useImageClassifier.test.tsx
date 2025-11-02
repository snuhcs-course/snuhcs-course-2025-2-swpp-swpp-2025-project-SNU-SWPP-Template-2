describe("useImageClassifier", () => {
    afterEach(() => {
        jest.resetModules();
        jest.clearAllMocks();
    });

    it("should try again if classifier is initially undefined", async () => {
        let i = 0;
        jest.mock('@infinitered/react-native-mlkit-image-labeling', () => ({
            useImageLabeling: (modelName: string) => {
                if (i == 0) {
                    return undefined;
                }

                return { classifyImage: jest.fn() };
            },
        }));
        const { useImageClassifier } = require("./useImageClassifier");
        const { isFoodImage } = useImageClassifier();
        expect(isFoodImage).toBeDefined();
    });

    it("should report classification results of library", async () => {
        jest.mock('@infinitered/react-native-mlkit-image-labeling', () => ({
            useImageLabeling: (modelName: string) => {
                async function mockClassifyImage(imageUri: string) {
                    return [{ confidence: 8.5, label: 0, text: "food" }]
                }

                return { classifyImage: mockClassifyImage };
            },
        }));
        const { useImageClassifier } = require("./useImageClassifier");
        const { isFoodImage } = useImageClassifier();
        expect(isFoodImage).toBeInstanceOf(Function);
        expect(await isFoodImage("exampleUri")).toBe(true);
    });
    it("should return null if the library doesn't work", async () => {
        jest.doMock('@infinitered/react-native-mlkit-image-labeling', () => ({
            useImageLabeling: (modelName: string) => ({
                classifyImage: async () => {
                    throw new Error("model failed");
                },
            }),
        }));
        const { useImageClassifier } = require("./useImageClassifier");
        const { isFoodImage } = useImageClassifier();
        expect(isFoodImage).toBeInstanceOf(Function);
        expect(await isFoodImage("exampleUri")).toBe(null);
    })
});