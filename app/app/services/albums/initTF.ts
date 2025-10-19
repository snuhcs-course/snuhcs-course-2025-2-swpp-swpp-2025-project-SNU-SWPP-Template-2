import * as tf from '@tensorflow/tfjs';
import '@tensorflow/tfjs-react-native';
import { bundleResourceIO } from '@tensorflow/tfjs-react-native';

export let isTFInitialized = false;
export let model: tf.LayersModel;

export async function initTF() {
    await tf.ready();
    isTFInitialized = true;

    const modelJson = require('assets/web_model/model.json');
    const modelWeights = require('assets/web_model/group1-shard1of1.bin');
    console.log('Loading model...');
    model = await tf.loadLayersModel(bundleResourceIO(modelJson, modelWeights));

    console.log("model's input shape:", model.inputs[0].shape);
    console.log('TensorFlow is ready, backend:', tf.getBackend());

}
