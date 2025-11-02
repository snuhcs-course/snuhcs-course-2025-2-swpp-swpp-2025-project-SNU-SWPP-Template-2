// Mock the ML Kit module at the top level
import { useImageClassifier } from "./useImageClassifier";

jest.mock('@infinitered/react-native-mlkit-image-labeling', () => ({
    useImageLabeling: jest.fn(),
}), { virtual: true });

describe("useImageClassifier", () => {
    const mockUseImageLabeling = require('@infinitered/react-native-mlkit-image-labeling').useImageLabeling;

    beforeEach(() => {
        jest.clearAllMocks();
    });

    it("should handle when classifier is initially undefined", () => {
        mockUseImageLabeling.mockReturnValue(undefined);
        const result = useImageClassifier();
        expect(result.isFoodImage).toBeDefined();
    });

    it("should report classification results of library", async () => {
        const mockClassifyImage = jest.fn().mockResolvedValue([
            { confidence: 8.5, label: 0, text: "food" }
        ]);
        
        mockUseImageLabeling.mockReturnValue({
            classifyImage: mockClassifyImage
        });
        
        const { isFoodImage } = useImageClassifier();
        expect(isFoodImage).toBeInstanceOf(Function);
        
        const result = await isFoodImage("exampleUri");
        expect(result).toBe(true);
        expect(mockClassifyImage).toHaveBeenCalledWith("exampleUri");
    });

    it("should return null if the library doesn't work", async () => {
        const mockClassifyImage = jest.fn().mockRejectedValue(new Error("model failed"));
        
        mockUseImageLabeling.mockReturnValue({
            classifyImage: mockClassifyImage
        });
        
        const { isFoodImage } = useImageClassifier();
        expect(isFoodImage).toBeInstanceOf(Function);
        
        const result = await isFoodImage("exampleUri");
        expect(result).toBe(null);
    });
});