import { Asset } from "expo-media-library";

export function getImage(asset: Asset) {
    return asset.uri.split('/').pop() || `image_${asset.id}.jpg`;
}