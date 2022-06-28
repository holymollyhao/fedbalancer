import os
import argparse
import numpy as np
import json
import pandas as pd

def preprocess(dataset_name: str):

    json_filename = [filename for filename in os.listdir(f'./leaf/data/{dataset_name}/data/train/') if filename.endswith('.json')][0]
    train_filename = json_filename
    test_filename = json_filename.replace('train', 'test')

    # file descriptor of train & test
    trn_f = open(f'./leaf/data/{dataset_name}/data/train/{train_filename}')
    tst_f = open(f'./leaf/data/{dataset_name}/data/test/{test_filename}')

    # load train & test json
    trn = json.load(trn_f)
    tst = json.load(tst_f)



    # enumerate for each user in training
    for user_idx, trn_user in enumerate(trn['users']):

        user_samples_location_text = []

        for sample_idx in range(len(trn['user_data'][trn_user]['x'])):

            # take sample for each user data
            sample_data = trn['user_data'][trn_user]['x'][sample_idx]
            sample_label = trn['user_data'][trn_user]['y'][sample_idx]

            cur_path = f"{dataset_name}/train/" + str(sample_label)
            os.makedirs(cur_path, exist_ok=True)

            with open(cur_path + "/user" + str(user_idx) + "_sample" + str(sample_idx) + ".txt",
                      "w") as f:
                f.write(",".join(str(x) for x in sample_data))
                f.close()
            user_samples_location_text.append(
                "train/" + str(sample_label) + "/user" + str(user_idx) + "_sample" + str(sample_idx) + ".txt")

        os.makedirs(f'./{dataset_name}', exist_ok=True)
        with open(f"{dataset_name}/user" + str(user_idx) + "_train.txt", "w") as f:
            f.write('\n'.join(user_samples_location_text))
            f.close()


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Process some integers.')
    parser.add_argument('--dataset', type=str, default='femnist', help='name of dataset')

    args = parser.parse_args()
    preprocess(args.dataset)
