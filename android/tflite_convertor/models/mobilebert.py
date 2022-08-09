# IMG_SIZE = 28
import tensorflow as tf
import argparse
from transformers import BertTokenizer, TFBertModel, TFBertForSequenceClassification

class TFBertforflwr2(tf.Module):

  def __init__(self):
      import bert

      model_dir = ".models/uncased_L-12_H-768_A-12"

      bert_params = bert.params_from_pretrained_ckpt(model_dir)
      l_bert = bert.BertModelLayer.from_params(bert_params, name="bert")

      model = tf.keras.models.Sequential([
          tf.keras.layers.InputLayer(input_shape=(128,)),
          l_bert,
          tf.keras.layers.Lambda(lambda x: x[:, 0, :]),
          tf.keras.layers.Dense(2)
      ])
      model.build(input_shape=(None, 128))

      # self.model = TFBertForSequenceClassification.from_pretrained('bert-base-uncased')
      self.model = model

      optimizer = tf.keras.optimizers.Adam(learning_rate=3e-5)
      loss = tf.keras.losses.SparseCategoricalCrossentropy(from_logits=True)
      self.model.compile(optimizer=optimizer, loss=loss)

  # The `train` function takes a batch of input images and labels.
  # @tf.function(input_signature=[{
  #     "inputs_embeds": tf.TensorSpec((None, None, 768), tf.float32, name="inputs_embeds"),
  #     # "attention_mask": tf.TensorSpec((None, None), tf.int32, name="attention_mask"),
  #     # "token_type_ids": tf.TensorSpec((None, None), tf.int32, name="token_type_ids")
  # }])
  @tf.function(input_signature=[
      tf.TensorSpec([None, None, 768], tf.float32),
      tf.TensorSpec([None, 2], tf.float32),
  ])
  def train(self, x, y):
    with tf.GradientTape() as tape:
      prediction = self.model(x)
      loss = self.model.loss(y, prediction)
    gradients = tape.gradient(loss, self.model.trainable_variables)
    self.model.optimizer.apply_gradients(
        zip(gradients, self.model.trainable_variables))
    result = {"loss": loss}
    return result

  @tf.function(input_signature=[
      tf.TensorSpec((None, None, 768), tf.float32, name="inputs_embeds"),
  ])
  def infer(self, x):
    logits = self.model(x)
    probabilities = tf.nn.softmax(logits, axis=-1)
    return {
        "output": probabilities,
        "logits": logits
    }

  @tf.function(input_signature=[tf.TensorSpec(shape=[], dtype=tf.string)])
  def save(self, checkpoint_path):
    tensor_names = [weight.name for weight in self.model.weights]
    tensors_to_save = [weight.read_value() for weight in self.model.weights]
    tf.raw_ops.Save(
        filename=checkpoint_path, tensor_names=tensor_names,
        data=tensors_to_save, name='save')
    return {
        "checkpoint_path": checkpoint_path
    }

  @tf.function(input_signature=[tf.TensorSpec(shape=[], dtype=tf.string)])
  def restore(self, checkpoint_path):
    restored_tensors = {}
    for var in self.model.weights:
      restored = tf.raw_ops.Restore(
          file_pattern=checkpoint_path, tensor_name=var.name, dt=var.dtype,
          name='restore')
      var.assign(restored)
      restored_tensors[var.name] = restored
    return restored_tensors
