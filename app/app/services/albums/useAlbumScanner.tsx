import * as MediaLibrary from "expo-media-library";
import { useImageClassifier } from "./useImageClassifier";
import { uploadData } from "aws-amplify/storage";
import { api } from "../api";
import { getImage } from "app/utils/imagenameFromAsseturi";
import { userAuthFacade } from "../registration/UserAuthFacade";
import * as storage from "app/utils/storage";

export function useAlbumScanner() {
    const { isFoodImage } = useImageClassifier();
    const [permissionResponse, requestPermission] = MediaLibrary.usePermissions(
        { granularPermissions: ['photo'] }
    );

    async function sendImageToS3(albumTitle: string, asset: MediaLibrary.Asset) {
        const filename = getImage(asset);
        console.log(`Trying to send image to S3: ${filename}`);

        // Check authentication status before attempting S3 upload
        const authStatus = await userAuthFacade.checkAuthenticationStatus();
        if (!authStatus.isAuthenticated) {
            console.log('Error: User not authenticated with AWS Cognito, attempting to re-authenticate...');
            
            // Try to re-authenticate the user
            try {
                // Get stored credentials and attempt sign-in
                const storedUsername = await storage.loadString("STORED_USERNAME");
                const storedPassword = await storage.loadString("STORED_PASSWORD");
                
                if (storedUsername && storedPassword) {
                    console.log(`Attempting to re-authenticate user: ${storedUsername}`);
                    const loginResult = await userAuthFacade.loginUser({
                        username: storedUsername,
                        password: storedPassword
                    });
                    
                    if (loginResult.success) {
                        console.log('Re-authentication successful, proceeding with S3 upload');
                    } else {
                        throw new Error(`Re-authentication failed: ${loginResult.errorMessage}`);
                    }
                } else {
                    throw new Error('No stored credentials found for re-authentication');
                }
            } catch (reAuthError) {
                console.log('Re-authentication failed:', reAuthError);
                console.log('Skipping S3 upload - user needs to log in again');
                return { photoUrl: "", imageBlob: null };
            }
        }
        
        console.log(`AWS Cognito authenticated as: ${authStatus.username}`);

        const response = await fetch(asset.uri);
        const blob = await response.blob();

        let photoUrl = "";
        try {
            const s3_result = await uploadData({
                path: ({ identityId }) => `protected/${identityId}/album/${albumTitle}/${filename}`,
                data: blob,
            }).result;
            photoUrl = s3_result.path;
            console.log('Sent image to S3: ', s3_result);
        } catch (error) {
            console.log('Error trying to send image to S3: ', error);
        }
        return { photoUrl, imageBlob: blob };
    }

    async function processImage(album: MediaLibrary.Album, asset: MediaLibrary.Asset, existingPhotoUris: Set<string>, onFoodFound: (asset: MediaLibrary.Asset) => void) {
        // Check if this image already exists in the database
        if (existingPhotoUris.has(asset.uri)) {
            console.log(`Image already exists in database, skipping: ${asset.uri}`);
            return;
        }

        const isFood = await isFoodImage(asset.uri);
        if (!isFood) {
            return;
        }

        const { photoUrl, imageBlob } = await sendImageToS3(album.title, asset);
        if (photoUrl == "" || !imageBlob) {
            console.log("Skipping photo upload - S3 upload failed or user not authenticated");
            return;
        }

        const response = await api.uploadPhoto(photoUrl, asset.uri, imageBlob);
        if (response.ok) {
            console.log("Sent photo url to server");
            onFoodFound(asset);
            // Add the new photo URI to the set to prevent future duplicates in this session
            existingPhotoUris.add(asset.uri);
        } else {
            console.error("Failed to send photo url to server:", response.problem);
        }
    }

    async function scanAlbum(album: MediaLibrary.Album, existingPhotoUris: Set<string>, onFoodFound: (asset: MediaLibrary.Asset) => void) {
        let endCursor;
        let hasNextPage = true;

        console.log(`Processing album: ${album.title}`);
        while (hasNextPage) {
            const result = await MediaLibrary.getAssetsAsync({
                album,
                first: 100,
                after: endCursor,
                sortBy: ['creationTime'],
            });
            endCursor = result.endCursor;
            hasNextPage = result.hasNextPage;

            for (const asset of result.assets) {
                await processImage(album, asset, existingPhotoUris, onFoodFound);
            }
        }
    }

    async function scanAlbums(onFoodFound: (asset: MediaLibrary.Asset) => void, onCompleted?: (foundCount: number) => void) {
        console.log(permissionResponse);
        if (permissionResponse?.status !== 'granted') {
            await requestPermission();
        }
        
        // Fetch existing photos once to check for duplicates
        let existingPhotoUris: Set<string> = new Set();
        try {
            const existingPhotos = await api.getUserPhotos();
            existingPhotoUris = new Set(existingPhotos.map(photo => photo.local_uri).filter(Boolean));
            console.log(`Found ${existingPhotoUris.size} existing photos in database`);
        } catch (error) {
            console.log("Error fetching existing photos:", error);
            // Continue with empty set if we can't fetch existing photos
        }
        
        const fetchedAlbums = await MediaLibrary.getAlbumsAsync({
            includeSmartAlbums: true,
        });
        
        let foundImageCount = 0;
        const trackFoundImages = (asset: MediaLibrary.Asset) => {
            foundImageCount++;
            onFoodFound(asset);
        };
        
        // Process albums sequentially and wait for completion
        for (const album of fetchedAlbums) {
            if (album.assetCount == 0) {
                continue; // skip empty albums
            }
            await scanAlbum(album, existingPhotoUris, trackFoundImages);
        }
        
        console.log(`Album scanning completed. Found ${foundImageCount} new food images.`);
        if (onCompleted) {
            onCompleted(foundImageCount);
        }
    }

    return { scanAlbums };
};