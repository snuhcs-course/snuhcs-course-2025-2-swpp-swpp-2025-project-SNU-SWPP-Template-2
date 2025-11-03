import { ImageLabelingConfig } from "@infinitered/react-native-mlkit-image-labeling";

export const MODELS: ImageLabelingConfig = {
  foodNonFood: {
    model: require("assets/models/best_model_with_metadata.tflite"),
    options: {
      maxResultCount: 1,
      confidenceThreshold: 0.0,
    },
  },
};