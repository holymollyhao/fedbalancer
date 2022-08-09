from transformers import TFBertForSequenceClassification
import tensorflow as tf

class TFBertforflwr(TFBertForSequenceClassification):
    # Decorate the serving method with the new input_signature
    # an input_signature represents the name, the data type and the shape of an expected input
    @tf.function(input_signature=[{
        "inputs_embeds": tf.TensorSpec((None, None, 768), tf.float32, name="inputs_embeds"),
        "attention_mask": tf.TensorSpec((None, None), tf.int32, name="attention_mask"),
        "token_type_ids": tf.TensorSpec((None, None), tf.int32, name="token_type_ids"),
    }])
    def serving(self, inputs):
        # call the model to process the inputs
        output = self.call(inputs)

        # return the formated output
        return self.serving_output(output)
