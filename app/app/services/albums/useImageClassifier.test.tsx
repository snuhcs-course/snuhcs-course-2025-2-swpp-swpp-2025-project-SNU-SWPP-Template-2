import { useImageLabeling } from '@infinitered/react-native-mlkit-image-labeling'

const mockUseImageLabeling = useImageLabeling as jest.MockedFunction<typeof useImageLabeling>

describe("useImageClassifier", () => {
    afterEach(() => {
        jest.clearAllMocks();
    });

    it("should try again if classifier is initially undefined", async () => {
        let i = 0;
        mockUseImageLabeling.mockImplementation((modelName: string) => {
            if (i === 0) {
                i++;
                return undefined;
            }
            return { classifyImage: jest.fn() };
        });
        
        const { useImageClassifier } = require("./useImageClassifier");
        const { isFoodImage } = useImageClassifier();
        expect(isFoodImage).toBeDefined();
    });

    it("should report classification results of library", async () => {
        const mockClassifyImage = jest.fn().mockResolvedValue([{ confidence: 8.5, label: 0, text: "food" }]);
        mockUseImageLabeling.mockReturnValue({ classifyImage: mockClassifyImage });
        
        const { useImageClassifier } = require("./useImageClassifier");
        const { isFoodImage } = useImageClassifier();
        expect(isFoodImage).toBeInstanceOf(Function);
        expect(await isFoodImage("exampleUri")).toBe(true);
    });

    it("should return null if the library doesn't work", async () => {
        const mockClassifyImage = jest.fn().mockRejectedValue(new Error("model failed"));
        mockUseImageLabeling.mockReturnValue({ classifyImage: mockClassifyImage });
        
        const { useImageClassifier } = require("./useImageClassifier");
        const { isFoodImage } = useImageClassifier();
        expect(isFoodImage).toBeInstanceOf(Function);
        expect(await isFoodImage("exampleUri")).toBe(null);
    });
});