import argparse

import tensorflow as tf
import tensorflow_addons as tfa
from tensorflow.keras import layers
from tensorflow.keras.regularizers import l2
from tfltransfer import bases
from tfltransfer import heads
from tfltransfer import optimizers
from tfltransfer.tflite_transfer_converter import TFLiteTransferConverter
import argparse
# from transformers import BertTokenizer, TFBertModel, TFBertForSequenceClassification
# from models.tfbertforseqclassification import TFBertforflwr
# from models.mobilebert import TFBertforflwr2
# import keras
# from tensorflow import keras
# from tensorflow.keras import layers


def convert_to_tflite(model:str):

    """Define the base model.

    To be compatible with TFLite Model Personalization, we need to define a
    base model and a head model.

    Here we are using an identity layer for base model, which just passes the
    input as it is to the head model.
    """
    if 'har' in model:
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
        converter.convert_and_save("tflite_model_har")

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
        head.compile(loss="categorical_crossentropy", optimizer="sgd")

        base_path = bases.saved_model_base.SavedModelBase("identity_model")
        converter = TFLiteTransferConverter(
            64, base_path, heads.KerasModelHead(head), optimizers.SGD(5e-3), train_batch_size=10
        )
        converter.convert_and_save("tflite_model")

    elif 'bert_tf2_ver' in model:

        from bert import BertModelLayer # pip insatll bert-tf2

        model_dir = ".models/uncased_L-12_H-768_A-12"
        l_bert = BertModelLayer(**BertModelLayer.Params(
            vocab_size=16000,  # embedding params
            use_token_type=True,
            use_position_embeddings=True,
            token_type_vocab_size=2,

            num_layers=12,  # transformer encoder params
            hidden_size=768,
            hidden_dropout=0.1,
            intermediate_size=4 * 768,
            intermediate_activation="gelu",

            adapter_size=None,  # see arXiv:1902.00751 (adapter-BERT)

            shared_layer=False,  # True for ALBERT (arXiv:1909.11942)
            embedding_size=None,  # None for BERT, wordpiece embedding size for ALBERT
        ))

        head = tf.keras.models.Sequential([
            tf.keras.layers.InputLayer(input_shape=(128,)),
            l_bert,
            tf.keras.layers.Lambda(lambda x: x[:, 0, :]),
            tf.keras.layers.Dense(2)
        ])
        print(head.summary())
        head.compile(loss="categorical_crossentropy", optimizer="sgd")

        base_path = bases.saved_model_base.SavedModelBase("identity_model")
        converter = TFLiteTransferConverter(
            2, base_path, heads.KerasModelHead(head), optimizers.SGD(5e-3), train_batch_size=16
        )
        converter.convert_and_save("tflite_model")

    elif 'resnet' in model:

        shape = (224, 224, 3) # in W, H, C W,H must be bigger than 34
        num_class = 1000

        base = tf.keras.Sequential(
            [tf.keras.Input(shape=shape), tf.keras.layers.Lambda(lambda x: x)]
        )

        base.compile(loss="categorical_crossentropy", optimizer="sgd")
        base.save("identity_model_resnet", save_format="tf")


        head = tf.keras.models.Sequential([
            tf.keras.applications.resnet.ResNet101(
                include_top=False,
                weights=None,
                input_tensor=None,
                input_shape=shape,
                pooling=None,
                classes=num_class,
            ),
            tf.keras.layers.Flatten(),
            tf.keras.layers.Dense(units=num_class, activation="softmax"),
        ])

        print(head.summary())
        head.compile(loss="categorical_crossentropy", optimizer="sgd")
        base_path = bases.saved_model_base.SavedModelBase("identity_model")
        converter = TFLiteTransferConverter(
            num_class, base_path, heads.KerasModelHead(head), optimizers.SGD(5e-3), train_batch_size=64
        )
        converter.convert_and_save("tflite_model")

    elif 'bert' in model:
        raise NotImplementedError

        # base = tf.keras.Sequential(
        #     [tf.keras.Input(shape=(None, None, 768)), tf.keras.layers.Lambda(lambda x: x)]
        # )
        #
        # base.compile(loss="categorical_crossentropy", optimizer="sgd")
        # base.save("identity_model_bert", save_format="tf")

        # import bert
        #
        # model_dir = ".models/uncased_L-12_H-768_A-12"
        #
        # bert_params = bert.params_from_pretrained_ckpt(model_dir)
        # l_bert = BertModelLayer.from_params(bert_params, name="bert")

        # head = keras.models.Sequential([
        #     keras.layers.InputLayer(input_shape=(128,)),
        #     l_bert,
        #     keras.layers.Lambda(lambda x: x[:, 0, :]),
        #     keras.layers.Dense(2)
        # ])
        # head.build(input_shape=(None, 128))


        # https: // github.com / huggingface / tflite - android - transformers / blob / master / models_generation / distilbert.py
        # model = TFBertForSequenceClassification.from_pretrained('bert-base-uncased')
        # input_spec = tf.TensorSpec([None, 764], tf.int32)
        # model._set_inputs(input_spec)
        #
        # converter = tf.lite.TFLiteConverter.from_keras_model(head)
        #
        # # For normal conversion:
        # converter.target_spec.supported_ops = [tf.lite.OpsSet.SELECT_TF_OPS]
        # tflite_model = converter.convert()
        # open("tfbert_for_seq_classification.tflite", "wb").write(tflite_model)
        # head = TFBertforflwr.from_pretrained('bert-base-uncased')

        # head = TFBertforflwr2()
        # head2 = TFBertForSequenceClassification.from_pretrained('bert-base-uncased')
        #
        # print(head.model.summary())
        # optimizer = tf.keras.optimizers.Adam(learning_rate=3e-5)
        # loss = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)
        # head.compile(optimizer=optimizer, loss=loss)
        # # print(config)
        # head.compile(loss=tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True))

        # base_path = bases.saved_model_base.SavedModelBase("identity_model")
        # optimizer = tf.keras.optimizers.Adam(learning_rate=3e-5)
        # converter = TFLiteTransferConverter(2, base_path, heads.KerasModelHead(head), optimizer, train_batch_size=10)

        # print(head.train.get_concrete_function())
        # converter = TFLiteTransferConverter(6, base_path, heads.KerasModelHead(head), optimizers.SGD(5e-3), train_batch_size=10)
        # converter.convert_and_save("tflite_model")

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


