#include "ConfigReader.h"

ConfigReader::~ConfigReader()
{
	if (fXMLDoc)
		delete fXMLDoc;
}

TiXmlElement* ConfigReader::getNamedNode(string inNode)
{
	TiXmlElement* result = NULL;

	TiXmlElement* root = fXMLDoc->FirstChildElement();
	if (root)
	{
		char* nodeBuffer = new char[inNode.length() + 1];
		if (nodeBuffer)
		{
			strcpy(nodeBuffer, inNode.c_str());
			char* node = strtok(nodeBuffer, "/");
			if (node)
			{
				result = root->FirstChildElement(node);
				while (result && (node = strtok(NULL, "/")))
				{
					result = result->FirstChildElement(node);
					if (result == NULL)
						break;
				}
			}

			delete [] nodeBuffer;
		}
	}

	return result;
}

bool ConfigReader::init(string inConfigFilename)
{
	fXMLDoc = new TiXmlDocument(inConfigFilename.c_str());
	if (fXMLDoc)
		fDocLoaded = fXMLDoc->LoadFile();

	return fDocLoaded;
}

bool ConfigReader::getString(string inNode, string& ioValue)
{
	if (!fDocLoaded)
		return false;

	bool result = false;
	ioValue = "";

	TiXmlElement* element = getNamedNode(inNode);
	if (element)
	{
		const char* text = element->GetText();
		if (text)
			ioValue = text;

		result = true;
	}

	return result;
}

bool ConfigReader::getConfigValueList(string inNode, string inItemTag, string& ioValue)
{
	if (!fDocLoaded)
		return false;

	bool result = false;
	ioValue = "";

	TiXmlElement* parent = getNamedNode(inNode);
	if (parent)
	{
		result = true;
		TiXmlNode* child = NULL;
		while (child = parent->IterateChildren(child))
		{
			TiXmlElement* theChildElement = child->ToElement();
			if (theChildElement)
			{
				string val = theChildElement->Value();
				if (val == inItemTag)
				{
					ioValue.append(theChildElement->GetText());
					ioValue.append(";");
				}
			}
		}

		// Strip trailing semicolon
		if (ioValue.length() > 0)
			ioValue = ioValue.substr(0, ioValue.length() - 1);
	}

	return result;
}

bool ConfigReader::getBoolean(string inNode)
{
	bool result = false;

	string strVal;
	if (getString(inNode, strVal))
		result = (_stricmp(strVal.c_str(), "true") == 0);

	return result;
}

int ConfigReader::getInteger(string inNode)
{
	int result = 0;

	string strVal;
	if (getString(inNode, strVal))
		result = atoi(strVal.c_str());

	return result;
}

int ConfigReader::translateJDKPrefString(string inPreference)
{
	int result = 0;

	if (inPreference == "jreOnly")
		result = 0;
	else if (inPreference == "preferJre")
		result = 1;
	else if (inPreference == "preferJdk")
		result = 2;
	else if (inPreference == "jdkOnly")
		result = 3;

	return result;
}

int ConfigReader::translatePriorityString(string inPriority)
{
	int result = 32;

	if (inPriority == "normal")
		result = 32;
	else if (inPriority == "idle")
		result = 64;
	else if (inPriority == "high")
		result = 128;

	return result;
}