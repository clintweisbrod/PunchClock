#include "tinyxml.h"
#include <string>

using namespace std;

class ConfigReader
{
	private:
		TiXmlDocument*	fXMLDoc;
		bool			fDocLoaded;

		TiXmlElement*	getNamedNode(string inNode);

	public:
		ConfigReader() {};
		~ConfigReader();

		bool init(string inConfigFilename);
		bool getString(string inNode, string& ioValue);
		bool getConfigValueList(string inNode, string inItemTag, string& ioValue);
		bool getBoolean(string inNode);
		int getInteger(string inNode);
		int translateJDKPrefString(string inPreference);
		int translatePriorityString(string inPriority);
};
