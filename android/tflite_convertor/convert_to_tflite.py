import argparse

import tensorflow as tf
from tensorflow.keras import layers
from tensorflow.keras.regularizers import l2
from tfltransfer import bases
from tfltransfer import heads
from tfltransfer import optimizers
from tfltransfer.tflite_transfer_converter import TFLiteTransferConverter
import argparse

def convert_to_tflite(model:str):

    """Define the base model.

    To be compatible with TFLite Model Personalization, we need to define a
    base model and a head model.

    Here we are using an identity layer for base model, which just passes the
    input as it is to the head model.
    """
    if 'uci_har' in model:
        base = tf.keras.Sequential(
            [tf.keras.Input(shape=(128, 9)), tf.keras.layers.Lambda(lambda x: x)]
        )

        base.compile(loss="categorical_crossentropy", optimizer="sgd")
        base.save("identity_model", save_format="tf")

        """Define the head model.

        This is the model architecture that we will train using Flower. 
        """
        head = tf.keras.Sequential(
            [
                tf.keras.Input(shape=(1152)),
                tf.keras.layers.Reshape((128, 9)),
                tf.keras.layers.Conv1D(filters=192, kernel_size=16, activation="relu", padding="same"),
                tf.keras.layers.MaxPooling1D(pool_size=4),
                tf.keras.layers.Flatten(),
                tf.keras.layers.Dense(units=256, activation="relu"),
                tf.keras.layers.Dense(units=6, activation="softmax"),
            ]
        )

        head.compile(loss="categorical_crossentropy", optimizer="sgd")
        print(head.summary())
        """Convert the model for TFLite.

        Using 10 classes in CIFAR10, learning rate = 1e-3 and batch size = 32

        This will generate a directory called tflite_model with five tflite models.
        Copy them in your Android code under the assets/model directory.
        """
        base_path = bases.saved_model_base.SavedModelBase("identity_model")
        converter = TFLiteTransferConverter(
            6, base_path, heads.KerasModelHead(head), optimizers.SGD(5e-3), train_batch_size=10
        )
        converter.convert_and_save("tflite_model")

    elif 'femnist' in model:

        base = tf.keras.Sequential(
            [tf.keras.Input(shape=(28, 28, 1)), tf.keras.layers.Lambda(lambda x: x)]
        )

        base.compile(loss="categorical_crossentropy", optimizer="sgd")
        base.save("identity_model_femnist", save_format="tf")

        """Define the head model.

        This is the model architecture that we will train using Flower. 
        """
        head = tf.keras.Sequential(
            [
                tf.keras.Input(shape=(784)),
                tf.keras.layers.Reshape((28, 28, 1)),
                tf.keras.layers.Conv2D(
                    filters=32,
                    kernel_size=[5, 5],
                    padding="same",
                    activation=tf.nn.relu
                ),
                tf.keras.layers.MaxPooling2D(
                    pool_size=[2, 2],
                    strides=2
                ),
                tf.keras.layers.Conv2D(
                    filters=64,
                    kernel_size=[5, 5],
                    padding="same",
                    activation=tf.nn.relu
                ),
                tf.keras.layers.MaxPooling2D(
                    pool_size=[2, 2],
                    strides=2
                ),
                tf.keras.layers.Flatten(),
                tf.keras.layers.Dense(units=2048, activation="relu"),
                tf.keras.layers.Dense(units=64, activation="softmax"),
            ]
        )
        print(head.summary())
        # TODO: not sure if this is the right thing...
        head.compile(loss="categorical_crossentropy", optimizer="sgd")
        print("\n\n\n\n\n\nthisisit\n\n\n\n\n\n\n")

        base_path = bases.saved_model_base.SavedModelBase("identity_model")
        # TODO: default values from default.cfg
        converter = TFLiteTransferConverter(
            64, base_path, heads.KerasModelHead(head), optimizers.SGD(1e-2), train_batch_size=10
        )

        print("\n\n\n\n\n\nthisisit\n\n\n\n\n\n\n")

        converter.convert_and_save("tflite_model")

    print("\n\n\n\n\n\nconversion and save done\n\n\n\n\n\n\n")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='tflite converter')
    parser.add_argument(
        "--model",
        type=str,
        default='uci_har_model',
        help='model type specification',
    )
    args = parser.parse_args()
    convert_to_tflite(model=args.model)
    # parser.add_argument(
    #     "--lr",
    #     type=str,
    #     default='uci_har_model',
    #     help='model type specification',
    # )
    # parser.add_argument(
    #     "--lr",
    #     type=str,
    #     default='uci_har_model',
    #     help='model type specification',
    # )


