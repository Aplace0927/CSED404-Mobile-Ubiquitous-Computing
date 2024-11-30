from libsvm.svmutil import *

y, x = svm_read_problem("sensor_data_normalized")

SPLIT = round(len(y) * 0.8)

m = svm_train(y[:SPLIT], x[:SPLIT], "-s 0 -t 2")
p_label, p_acc, p_val = svm_predict(y[SPLIT:], x[SPLIT:], m)

svm_save_model("model_file", m)

print(y)
print(p_label)
