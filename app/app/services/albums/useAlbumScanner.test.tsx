import { waitFor } from "@testing-library/react-native";

jest.mock('expo-media-library', () => ({
    usePermissions: jest.fn(),
    getAlbumsAsync: jest.fn(),
    getAssetsAsync: jest.fn()
}));
jest.mock('@infinitered/react-native-mlkit-image-labeling', () => ({
    useImageLabeling: jest.fn(),
}), { virtual: true });
jest.mock('aws-amplify/storage', () => ({
    uploadData: jest.fn(),
}));
jest.mock('../api', () => ({
    api: {
        uploadPhoto: jest.fn()
    }
}));

describe("useAlbumScanner", () => {
    let MediaLibrary: any;
    let useAlbumScanner: any;
    let useImageLabeling: any;
    let uploadData: any;
    let api: any;

    beforeEach(() => {
        jest.resetModules();
        global.fetch = jest.fn().mockResolvedValue({ blob: jest.fn() });
        MediaLibrary = require("expo-media-library");
        useAlbumScanner = require("./useAlbumScanner").useAlbumScanner;
        useImageLabeling = require("@infinitered/react-native-mlkit-image-labeling").useImageLabeling;
        uploadData = require("aws-amplify/storage").uploadData;
        api = require("../api").api;
    });

    afterEach(() => {
        jest.clearAllMocks();
    });

    it("should ask for permission if not granted", async () => {
        const mockRequestPermission = jest.fn();
        MediaLibrary.usePermissions.mockReturnValue([{ status: "denied" }, mockRequestPermission]);
        MediaLibrary.getAlbumsAsync.mockResolvedValue([{ assetCount: 0 }]);

        const { scanAlbums } = useAlbumScanner();
        await scanAlbums(() => { });
        expect(mockRequestPermission).toHaveBeenCalled();
    });
    it("should skip empty albums", async () => {
        MediaLibrary.usePermissions.mockReturnValue([{ status: "granted" }, jest.fn()]);
        MediaLibrary.getAlbumsAsync.mockResolvedValue([{ assetCount: 0 }]);

        const { scanAlbums } = useAlbumScanner();
        await scanAlbums(() => { });
        expect(MediaLibrary.getAssetsAsync).not.toHaveBeenCalled();
    });
    it("should do nothing if image is non-food", async () => {

        MediaLibrary.usePermissions.mockReturnValue([{ status: "granted" }, jest.fn()]);
        MediaLibrary.getAlbumsAsync.mockResolvedValue([{ assetCount: 1 }]);
        MediaLibrary.getAssetsAsync.mockResolvedValue({
            endCursor: 1,
            hasNextPage: false,
            assets: [{ uri: "testuri1" }]
        });
        useImageLabeling.mockReturnValue({
            classifyImage: async function mockClassifyImage(imageUri: string) {
                return [{ confidence: 8.5, label: 0, text: "non-food" }]
            }
        });

        const { scanAlbums } = useAlbumScanner();
        await scanAlbums(() => { });
        expect(uploadData).not.toHaveBeenCalled();
    });

    it("should upload images to S3 and server", async () => {
        MediaLibrary.usePermissions.mockReturnValue([{ status: "granted" }, jest.fn()]);
        MediaLibrary.getAlbumsAsync.mockResolvedValue([{ assetCount: 1 }]);
        MediaLibrary.getAssetsAsync.mockResolvedValue({
            endCursor: 1,
            hasNextPage: false,
            assets: [{ uri: "testuri1" }]
        });
        useImageLabeling.mockReturnValue({
            classifyImage: async function mockClassifyImage(imageUri: string) {
                return [{ confidence: 8.5, label: 0, text: "food" }]
            }
        });
        uploadData.mockReturnValue({
            result: new Promise((resolve, reject) => {
                resolve({
                    path: "photoUrl"
                })
            })
        });
        api.uploadPhoto.mockResolvedValue({
            ok: true
        })
        const { scanAlbums } = useAlbumScanner();
        await scanAlbums(() => { });

        await waitFor(() => {
            expect(uploadData).toHaveBeenCalled();
            expect(api.uploadPhoto).toHaveBeenCalled();
        })
    })
    it("should call given callback", async () => {
        MediaLibrary.usePermissions.mockReturnValue([{ status: "granted" }, jest.fn()]);
        MediaLibrary.getAlbumsAsync.mockResolvedValue([{ assetCount: 1 }]);
        MediaLibrary.getAssetsAsync.mockResolvedValue({
            endCursor: 1,
            hasNextPage: false,
            assets: [{ uri: "testuri1" }]
        });
        useImageLabeling.mockReturnValue({
            classifyImage: async function mockClassifyImage(imageUri: string) {
                return [{ confidence: 8.5, label: 0, text: "food" }]
            }
        });
        uploadData.mockReturnValue({
            result: new Promise((resolve, reject) => {
                resolve({
                    path: "photoUrl"
                })
            })
        });
        api.uploadPhoto.mockResolvedValue({
            ok: true
        })
        const { scanAlbums } = useAlbumScanner();
        const mockCallBack = jest.fn();
        await scanAlbums(mockCallBack);

        await waitFor(() => {
            expect(mockCallBack).toHaveBeenCalled();
        })
    })
});
