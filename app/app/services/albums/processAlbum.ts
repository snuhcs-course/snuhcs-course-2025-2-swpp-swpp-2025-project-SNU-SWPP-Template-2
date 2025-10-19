import * as MediaLibrary from 'expo-media-library';
import { uploadData } from 'aws-amplify/storage';
import { api } from '../api';
import * as tf from '@tensorflow/tfjs';
import { initTF, isTFInitialized, model } from './initTF';
import { decodeJpeg } from '@tensorflow/tfjs-react-native';
import * as FileSystem from 'expo-file-system';

// Define ItemWithPath type if not imported from elsewhere
type ItemWithPath = { path: string };


export async function processAlbum(
    album: MediaLibrary.Album,
    userImages: { id: string, type: string, image: any, name: string }[],
    setUserImages: React.Dispatch<React.SetStateAction<{
        id: string;
        type: string;
        image: any;
        name: string;
    }[]>>) {
    let endCursor = undefined;
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
            processImage(asset, album, userImages, setUserImages);
            return true;
        }
    }
}

async function processImage(
    asset: MediaLibrary.Asset,
    album: MediaLibrary.Album,
    userImages: { id: string, type: string, image: any, name: string }[],
    setUserImages: React.Dispatch<React.SetStateAction<{
        id: string;
        type: string;
        image: any;
        name: string;
    }[]>>) {
    // get image file
    const fileUri = asset.uri;
    const response = await fetch(fileUri);
    const blob = await response.blob();
    const base64 = await FileSystem.readAsStringAsync(fileUri, {
        encoding: FileSystem.EncodingType.Base64,
    });

    const filename = fileUri.split('/').pop() || `image_${asset.id}.jpg`;
    console.log(filename);

    // test if image is food using AI model
    if (!isTFInitialized) {
        await initTF();
    }
    const isFood = await isFoodImage(base64);
    if (!isFood) {
        console.log('Image is not classified as food. Skipping upload.');
        return;
    } else {
        console.log('Image is classified as food.');
    }

    let s3_result: ItemWithPath = { path: "" };
    try {
        // upload to S3
        s3_result = await uploadData({
            path: ({ identityId }) => `protected/${identityId}/album/${album.title}/${filename}`,
            data: blob,
            options: {
                onProgress: ({ transferredBytes, totalBytes }) => {
                    if (totalBytes) {
                        console.log(
                            `Upload progress ${Math.round((transferredBytes / totalBytes) * 100)
                            } %`
                        );
                    }
                }
            }
        }).result;
        console.log('Sent image to S3: ', s3_result);

    } catch (error) {
        console.log('Error trying to send image to S3: ', error);
    }

    // send url to db
    const photoUrl = s3_result?.path;
    console.log('Photo URL: ', photoUrl);

    await api.uploadPhoto(photoUrl);

    // add to userimages
    console.log(`Adding image ${fileUri} to user images`);
    setUserImages([...userImages, {
        id: asset.id,
        type: 'user',
        image: {uri: fileUri},
        name: filename,
    }]);
}

async function isFoodImage(base64: string): Promise<boolean> {
    const imageBuffer = tf.util.encodeString(base64, 'base64').buffer;
    console.log('Image buffer length: ', imageBuffer.byteLength);
    const rawImageData = new Uint8Array(imageBuffer);
    console.log('Raw image data length: ', rawImageData.length);
    const imageTensor = decodeJpeg(rawImageData).resizeBilinear([224, 224]).expandDims(0).toFloat().div(tf.scalar(255));
    console.log("imageTensor shape: ", imageTensor.shape);
    const output = model.predict(imageTensor) as tf.Tensor;
    const score = (await output.data())[0];

    imageTensor.dispose();
    output.dispose();

    console.log('Food classification score (0 is food): ', score);
    return score < 0.5;
}