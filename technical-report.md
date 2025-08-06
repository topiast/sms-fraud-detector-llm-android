The problem definition and idea of the project:
It is very common to receive scam messages via SMS, at least here in Finland. These messages are nowadays often very sophisticated and may be hard to identify.
While using traditional Machine Learning methods or even more sophisticated methods, the ability of an LLM model to explain why the message is fraud or not, was catching my interest.
So, I wanted to create a mobile app that uses an LLM model to detect SMS fraud messages. In the context of security and privacy, local llm models seem natural choice, since they do not require sending the messages to a server for processing.

Technical challenges:

1. Biggest challenge: Get good enough performance for the llm to do SMS fraud detection reliably. 
 - How it was solved: 
    - Dataset creation for evaluation & fine-tuning:
        - I used my own sms message history with about 1500 messages. I also had to somewhat manually filter scam messages from my own history. I also anonymized the messages to protect my privacy: by randomizing the phone numbers and names etc.
        - To deal with the class imbalance, I created a dataset with about 800 fraud messages using locally run gemma3:12b model. The challenge here was to create a dataset that was diverse. I used AI to create 52 different themes for the fraud messages. I also had 4 diffrent writing styles, 2 different languages, 3 different lengths, and 3 different message styles. I then randomly sampled with those defined parameters about 800 prompts which were used to further generate the messages.
        - For both datasets I used locally run gemma3:12b model to generate exmample answers to the messages. In the answers I wanted that the model provided and explanation why the message is fraud or not and an estimated score for the fraud probability.
        - So I ended up with a dataset consisting of 2612 messages.
   - Instruction tuning and prompting
        - This step consited of mulple iterations of changing the system prompt and evaluating the results.
        - It was quite obvious from the start that insturction tuning and prompting alone was not enough. The model was easily tricked and confused by different types of messages. It either flagged too many messages as fraud or too few.
        - I learned a lot of creating effective prompts and how to structure the input data for the model.
    - Fine-tuning the model
        - I wanted to try GRPO training the model with classification accuracy as the objective. However, I did not get good results with it. This might be due to the lack of GPU resources, since I had to use extremely memory efficient training arguments.
        - I ended up using peft(Parameter-Efficient Fine-Tuning) with LoRA (Low-Rank Adaptation). The results were much better and there was I clear improvement in classification accuracy. However, I still had many difficulties getting good performance. 
        - I had to revise the dataset multiple times and I find several issues with the dataset. E.g. In the anonymization process I had randomized all 4 digit codes which led to unlrealistic dates in the messages. I noticed that in the dataset creation the model had picked up on this and was confused by the dates. Additionally, I find many more issues with the quality of the dataset. 
        - After each iteration of fixing issues in the dataset, I almost always improved the performance of the model.

 - Evaluation & results

### Fine-tuned Model Results (Threshold = 0.5)
- Accuracy:  0.8740
- Precision: 0.7212
- Recall:    0.9494
- F1-score:  0.8197
- ROC-AUC:   0.9560
- PR-AUC:    0.8644
- Correct Format: 0.8702

**Classification Report:**
| Class         | Precision | Recall | F1-score | Support |
|---------------|-----------|--------|----------|---------|
| Legitimate(0) | 0.97      | 0.84   | 0.90     | 183     |
| Scam(1)       | 0.72      | 0.95   | 0.82     | 79      |

- Macro avg: Precision 0.85, Recall 0.90, F1-score 0.86
- Weighted avg: Precision 0.90, Recall 0.87, F1-score 0.88

### Base Model Results (Threshold = 0.5)
- Accuracy:  0.7519
- Precision: 0.5614
- Recall:    0.8101
- F1-score:  0.6632
- ROC-AUC:   0.8403
- PR-AUC:    0.6688
- Correct Format: 1.0000

**Classification Report:**
| Class         | Precision | Recall | F1-score | Support |
|---------------|-----------|--------|----------|---------|
| Legitimate(0) | 0.90      | 0.73   | 0.80     | 183     |
| Scam(1)       | 0.56      | 0.81   | 0.66     | 79      |

- Macro avg: Precision 0.73, Recall 0.77, F1-score 0.73
- Weighted avg: Precision 0.80, Recall 0.75, F1-score 0.76


2. Creating a Mobile app
    - Challenges:
        - I wanted to create a simple UI and automatic fraud detection for incoming messages. e.g. sms -> llm model -> notification.
        - I did not know much about Android development. Therefore I had to rely a lot on AI generated code.
        - I had to find a way to run the model on the background in a way that latency is reasonable.
        - I also had to address the issue of how to handling multiple messages at once without causing issues. To address this I used a queue system to handle the messages one by one.
        - Since running llm model everytime when there is an incomming message is maybe not the smartest way if the amount of messages is high, I wanted to implement a feature to turn off the automatic detection and allow manual detection. To implement this I used Android's native Share feature to share message text with the app. This way the user can share any message with the app and get a fraud detection result.

    - Implementation details:
        - Features:
            - Onboarding flow for permissions and privacy
            - Real-time SMS monitoring and fraud detection
            - Instant notifications for fraud alerts and safe messages
            - Local LLM model management and configuration
            - Message history and manual flagging
            - Share integration for analyzing arbitrary text

        - Architecture:
            The app uses a modular architecture built with Jetpack Compose for the UI. SMS messages are processed locally using LLM-based detection, with all analysis and data storage performed on-device to ensure privacy. Incoming SMS messages are handled one by one in the background using a queue system, which prevents resource conflicts and ensures reliable processing—especially important due to the latency of LLM inference. All detection and data storage are performed locally to maximize privacy and avoid sending any data externally.


3. Future improvements & know issues:
    - The model accuracy is not yet good enough for production use. Even though the performance improved a lot with fine-tuning, I noticed that even with false prediction the model is able to provide a reasonable explanation. However, the score is often not in line with the explanation or is set to 0.5. This is where GRPO training could be useful.
    - After fine tuning the model's ability to strictly follow the format requested decreased. This could be an issue with the dataset or the fine-tuning process that needs further investigation.
    - Due to technical challenges, and time constraints, the fine-tuned model is not deployed in the app at the time of submission. Instead, the app relies on google/gemma-3n-E4B-it model with instruction tuning.
            