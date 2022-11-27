db = {
    'user'     : 'root',
    'password' : 'nmsln1721',
    'host'     : '127.0.0.1',
    'port'     : '6603',
    'database' : 'fedtherapist'
}

DB_URL = f"mysql+mysqlconnector://{db['user']}:{db['password']}@{db['host']}:{db['port']}/{db['database']}?charset=utf8"

UPLOAD_FOLDER = './data/'
MAGPHASE_FOLDER = './magphase/'
VOICESAMPLE_FOLDER = './voiceSample/'
DVEC_FOLDER = './dvec/'
MODEL_FOLDER = './model/'
ALLOWED_EXTENSIONS = {'txt', 'zip'}
